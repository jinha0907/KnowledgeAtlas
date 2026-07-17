package com.projectkg.api.decision.repository;

import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DecisionExtractionRunRepository {
  private final JdbcTemplate jdbcTemplate;

  public DecisionExtractionRunRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public Optional<ExtractionRunRow> findByDocumentAndChecksum(long documentId, String sourceChecksum) {
    return jdbcTemplate.query(
            """
            SELECT id, document_id, source_checksum, status, extracted_decisions
            FROM decision_extraction_run
            WHERE document_id = ? AND source_checksum = ?
            """,
            (rs, rowNum) -> new ExtractionRunRow(
                rs.getLong("id"),
                rs.getLong("document_id"),
                rs.getString("source_checksum"),
                rs.getString("status"),
                rs.getInt("extracted_decisions")),
            documentId,
            sourceChecksum)
        .stream()
        .findFirst();
  }

  public long createRunning(long documentId, String sourceChecksum) {
    try {
      Long id = jdbcTemplate.queryForObject(
          """
          INSERT INTO decision_extraction_run (document_id, source_checksum, status)
          VALUES (?, ?, 'running')
          RETURNING id
          """,
          Long.class,
          documentId,
          sourceChecksum);
      if (id == null) {
        throw new IllegalStateException("Failed to create decision extraction run");
      }
      return id;
    } catch (DuplicateKeyException ex) {
      throw new DecisionExtractionAlreadyRunningException(documentId, ex);
    }
  }

  public void markSuccess(long runId, int extractedDecisions) {
    jdbcTemplate.update(
        """
        UPDATE decision_extraction_run
        SET status = 'success', extracted_decisions = ?, completed_at = NOW(), error_message = NULL
        WHERE id = ?
        """,
        extractedDecisions,
        runId);
  }

  public void markFailed(long runId, String errorMessage) {
    jdbcTemplate.update(
        """
        UPDATE decision_extraction_run
        SET status = 'failed', completed_at = NOW(), error_message = ?
        WHERE id = ?
        """,
        truncate(errorMessage, 1500),
        runId);
  }

  public void retryFailed(long runId) {
    jdbcTemplate.update(
        """
        UPDATE decision_extraction_run
        SET status = 'running', extracted_decisions = 0, completed_at = NULL, error_message = NULL
        WHERE id = ? AND status = 'failed'
        """,
        runId);
  }

  private String truncate(String value, int maxLength) {
    if (value == null || value.length() <= maxLength) {
      return value;
    }
    return value.substring(0, maxLength);
  }

  public record ExtractionRunRow(
      long id,
      long documentId,
      String sourceChecksum,
      String status,
      int extractedDecisions
  ) {}

  public static class DecisionExtractionAlreadyRunningException extends RuntimeException {
    public DecisionExtractionAlreadyRunningException(long documentId, Throwable cause) {
      super("Decision extraction is already running for document: " + documentId, cause);
    }
  }
}
