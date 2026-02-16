package com.projectkg.api.notion.repository;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ChunkRepository {
  private final JdbcTemplate jdbcTemplate;

  public ChunkRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public void replaceForBlock(long documentId, String blockId, List<ChunkRow> chunks) {
    jdbcTemplate.update("DELETE FROM chunk WHERE document_id = ? AND block_id = ?", documentId, blockId);

    String sql = """
        INSERT INTO chunk (
          document_id, block_id, chunk_index, text, token_count, checksum, created_at, updated_at
        )
        VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())
        """;

    for (ChunkRow chunk : chunks) {
      jdbcTemplate.update(
          sql,
          documentId,
          blockId,
          chunk.chunkIndex(),
          chunk.text(),
          chunk.tokenCount(),
          chunk.checksum());
    }
  }

  public record ChunkRow(int chunkIndex, String text, int tokenCount, String checksum) {}
}
