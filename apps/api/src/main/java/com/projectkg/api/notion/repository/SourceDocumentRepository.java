package com.projectkg.api.notion.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SourceDocumentRepository {
  private final JdbcTemplate jdbcTemplate;

  public SourceDocumentRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public Optional<SourceDocumentRow> findBySource(String sourceType, String sourceId) {
    String sql = """
        SELECT id, checksum
        FROM source_document
        WHERE source_type = ? AND source_id = ?
        """;

    return jdbcTemplate.query(sql,
            (rs, rowNum) -> new SourceDocumentRow(rs.getLong("id"), rs.getString("checksum")),
            sourceType,
            sourceId)
        .stream()
        .findFirst();
  }

  public long upsert(
      String sourceType,
      String sourceId,
      String title,
      Instant lastSyncedAt,
      String rawJson,
      String checksum
  ) {
    String sql = """
        INSERT INTO source_document (
          source_type, source_id, title, last_synced_at, raw_json, checksum, created_at, updated_at
        )
        VALUES (?, ?, ?, ?, CAST(? AS jsonb), ?, NOW(), NOW())
        ON CONFLICT (source_type, source_id)
        DO UPDATE SET
          title = EXCLUDED.title,
          last_synced_at = EXCLUDED.last_synced_at,
          raw_json = EXCLUDED.raw_json,
          checksum = EXCLUDED.checksum,
          updated_at = NOW()
        RETURNING id
        """;

    Long id = jdbcTemplate.queryForObject(
        sql,
        Long.class,
        sourceType,
        sourceId,
        title,
        Timestamp.from(lastSyncedAt),
        rawJson,
        checksum
    );

    if (id == null) {
      throw new IllegalStateException("Failed to upsert source_document");
    }
    return id;
  }

  public record SourceDocumentRow(long id, String checksum) {}
}
