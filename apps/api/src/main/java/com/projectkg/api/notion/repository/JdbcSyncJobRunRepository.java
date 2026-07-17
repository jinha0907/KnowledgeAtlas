package com.projectkg.api.notion.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcSyncJobRunRepository implements SyncJobRunRepository {
  private final JdbcTemplate jdbcTemplate;

  public JdbcSyncJobRunRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public long createRunning(String sourceType) {
    String sql = """
        INSERT INTO sync_job_run (source_type, started_at, status, synced_documents)
        VALUES (?, NOW(), 'running', 0)
        RETURNING id
        """;

    try {
      Long id = jdbcTemplate.queryForObject(sql, Long.class, sourceType);
      if (id == null) {
        throw new IllegalStateException("Failed to create sync_job_run");
      }
      return id;
    } catch (DuplicateKeyException ex) {
      throw new SyncAlreadyRunningException(sourceType, ex);
    }
  }

  @Override
  public void markSuccess(long id, int syncedDocuments, Instant sourceWatermarkAt) {
    String sql = """
        UPDATE sync_job_run
        SET finished_at = NOW(),
            status = 'success',
            synced_documents = ?,
            source_watermark_at = ?,
            error_message = NULL
        WHERE id = ?
        """;
    jdbcTemplate.update(sql, syncedDocuments, Timestamp.from(sourceWatermarkAt), id);
  }

  @Override
  public void markFailed(long id, String errorMessage) {
    String sql = """
        UPDATE sync_job_run
        SET finished_at = NOW(), status = 'failed', error_message = ?
        WHERE id = ?
        """;
    jdbcTemplate.update(sql, truncate(errorMessage, 1500), id);
  }

  @Override
  public Optional<Instant> findLatestSuccessfulSourceWatermark(String sourceType) {
    String sql = """
        SELECT COALESCE(source_watermark_at, finished_at) AS source_watermark_at
        FROM sync_job_run
        WHERE source_type = ? AND status = 'success' AND finished_at IS NOT NULL
        ORDER BY finished_at DESC
        LIMIT 1
        """;

    return jdbcTemplate.query(sql,
            (rs, rowNum) -> rs.getTimestamp("source_watermark_at"),
            sourceType)
        .stream()
        .findFirst()
        .map(Timestamp::toInstant);
  }

  public static class SyncAlreadyRunningException extends RuntimeException {
    public SyncAlreadyRunningException(String sourceType, Throwable cause) {
      super("A sync is already running for source type: " + sourceType, cause);
    }
  }

  private String truncate(String text, int maxLen) {
    if (text == null) {
      return null;
    }
    return text.length() <= maxLen ? text : text.substring(0, maxLen);
  }
}
