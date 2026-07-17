package com.projectkg.api.notion.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.List;
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

  public Optional<SourceDocumentDetailRow> findById(long documentId) {
    String sql = """
        SELECT id, title, checksum, last_synced_at
        FROM source_document
        WHERE id = ?
        """;

    return jdbcTemplate.query(sql,
            (rs, rowNum) -> new SourceDocumentDetailRow(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("checksum"),
                rs.getTimestamp("last_synced_at").toInstant()),
            documentId)
        .stream()
        .findFirst();
  }

  public List<SourceDocumentSummaryRow> findAll() {
    return jdbcTemplate.query(
        """
        SELECT id, source_type, source_id, title, last_synced_at, checksum
        FROM source_document
        ORDER BY last_synced_at DESC, id DESC
        """,
        (rs, rowNum) -> new SourceDocumentSummaryRow(
            rs.getLong("id"),
            rs.getString("source_type"),
            rs.getString("source_id"),
            rs.getString("title"),
            rs.getTimestamp("last_synced_at").toInstant(),
            rs.getString("checksum")));
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

  public record SourceDocumentDetailRow(long id, String title, String checksum, Instant lastSyncedAt) {}

  public record SourceDocumentSummaryRow(
      long id,
      String sourceType,
      String sourceId,
      String title,
      Instant lastSyncedAt,
      String checksum
  ) {}
}
