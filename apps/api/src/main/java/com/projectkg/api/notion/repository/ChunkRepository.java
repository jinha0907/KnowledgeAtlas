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

  public boolean replaceForBlock(long documentId, String blockId, List<ChunkRow> chunks) {
    if (matchesExistingChunks(documentId, blockId, chunks)) {
      return false;
    }

    deleteByDocumentAndBlock(documentId, blockId);

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
    return true;
  }

  private boolean matchesExistingChunks(long documentId, String blockId, List<ChunkRow> chunks) {
    List<ChunkRow> existing = jdbcTemplate.query(
        """
        SELECT chunk_index, text, token_count, checksum
        FROM chunk
        WHERE document_id = ? AND block_id = ?
        ORDER BY chunk_index ASC
        """,
        (rs, rowNum) -> new ChunkRow(
            rs.getInt("chunk_index"),
            rs.getString("text"),
            rs.getInt("token_count"),
            rs.getString("checksum")),
        documentId,
        blockId);
    return existing.equals(chunks);
  }

  public void deleteByDocumentAndBlock(long documentId, String blockId) {
    jdbcTemplate.update("DELETE FROM chunk WHERE document_id = ? AND block_id = ?", documentId, blockId);
  }

  public record ChunkRow(int chunkIndex, String text, int tokenCount, String checksum) {}
}
