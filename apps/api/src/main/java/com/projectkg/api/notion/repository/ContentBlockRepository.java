package com.projectkg.api.notion.repository;

import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ContentBlockRepository {
  private final JdbcTemplate jdbcTemplate;

  public ContentBlockRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public void upsert(long documentId, String blockId, String text, String path, Instant updatedAt) {
    String sql = """
        INSERT INTO content_block (
          document_id, block_id, text, path, updated_at, created_at
        )
        VALUES (?, ?, ?, ?, ?, NOW())
        ON CONFLICT (document_id, block_id)
        DO UPDATE SET
          text = EXCLUDED.text,
          path = EXCLUDED.path,
          updated_at = EXCLUDED.updated_at
        """;

    jdbcTemplate.update(sql, documentId, blockId, text, path, Timestamp.from(updatedAt));
  }
}
