package com.projectkg.api.analysis.repository;

import java.sql.Array;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DocumentAnalysisRunRepository {
  private final JdbcTemplate jdbcTemplate;

  public DocumentAnalysisRunRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public Optional<AnalysisRunRow> findByDocumentAndChecksum(long documentId, String checksum) {
    return jdbcTemplate.query(
            """
            SELECT id, document_id, source_checksum, status, summary, tags, completed_at
            FROM document_analysis_run
            WHERE document_id = ? AND source_checksum = ?
            """,
            (rs, rowNum) -> mapRow(rs.getLong("id"), rs.getLong("document_id"),
                rs.getString("source_checksum"), rs.getString("status"), rs.getString("summary"),
                rs.getArray("tags"), rs.getTimestamp("completed_at")),
            documentId,
            checksum)
        .stream()
        .findFirst();
  }

  public Optional<AnalysisRunRow> findLatestByDocumentId(long documentId) {
    return jdbcTemplate.query(
            """
            SELECT id, document_id, source_checksum, status, summary, tags, completed_at
            FROM document_analysis_run
            WHERE document_id = ?
            ORDER BY created_at DESC, id DESC
            LIMIT 1
            """,
            (rs, rowNum) -> mapRow(rs.getLong("id"), rs.getLong("document_id"),
                rs.getString("source_checksum"), rs.getString("status"), rs.getString("summary"),
                rs.getArray("tags"), rs.getTimestamp("completed_at")),
            documentId)
        .stream()
        .findFirst();
  }

  public long createRunning(long documentId, String checksum) {
    try {
      Long id = jdbcTemplate.queryForObject(
          """
          INSERT INTO document_analysis_run (document_id, source_checksum, status)
          VALUES (?, ?, 'running')
          RETURNING id
          """,
          Long.class,
          documentId,
          checksum);
      if (id == null) {
        throw new IllegalStateException("Failed to create document analysis run");
      }
      return id;
    } catch (DuplicateKeyException ex) {
      throw new AnalysisAlreadyRunningException(documentId, ex);
    }
  }

  public void markSuccess(long runId, String summary, List<String> tags) {
    jdbcTemplate.update(
        """
        UPDATE document_analysis_run
        SET status = 'success', summary = ?, tags = ?, completed_at = NOW(), error_message = NULL
        WHERE id = ?
        """,
        summary,
        tags.toArray(String[]::new),
        runId);
  }

  public void markFailed(long runId, String errorMessage) {
    jdbcTemplate.update(
        """
        UPDATE document_analysis_run
        SET status = 'failed', completed_at = NOW(), error_message = ?
        WHERE id = ?
        """,
        truncate(errorMessage, 1500),
        runId);
  }

  public void retryFailed(long runId) {
    jdbcTemplate.update(
        """
        UPDATE document_analysis_run
        SET status = 'running', summary = NULL, tags = ARRAY[]::TEXT[], completed_at = NULL,
            error_message = NULL
        WHERE id = ? AND status = 'failed'
        """,
        runId);
  }

  private AnalysisRunRow mapRow(
      long id,
      long documentId,
      String checksum,
      String status,
      String summary,
      Array sqlTags,
      Timestamp completedAt
  ) throws SQLException {
    String[] tagValues = sqlTags == null ? new String[0] : (String[]) sqlTags.getArray();
    return new AnalysisRunRow(
        id, documentId, checksum, status, summary, Arrays.asList(tagValues),
        completedAt == null ? null : completedAt.toInstant());
  }

  private String truncate(String value, int maxLength) {
    if (value == null || value.length() <= maxLength) {
      return value;
    }
    return value.substring(0, maxLength);
  }

  public record AnalysisRunRow(
      long id,
      long documentId,
      String sourceChecksum,
      String status,
      String summary,
      List<String> tags,
      Instant completedAt
  ) {}

  public static class AnalysisAlreadyRunningException extends RuntimeException {
    public AnalysisAlreadyRunningException(long documentId, Throwable cause) {
      super("Document analysis is already running for document: " + documentId, cause);
    }
  }
}
