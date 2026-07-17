package com.projectkg.api.embedding.repository;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcEmbeddingRepository implements EmbeddingRepository {
  private final JdbcTemplate jdbcTemplate;

  public JdbcEmbeddingRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public List<Long> findDocumentIdsWithMissingEmbeddings() {
    return jdbcTemplate.queryForList(
        """
        SELECT DISTINCT c.document_id
        FROM chunk c
        LEFT JOIN embedding e ON e.chunk_id = c.id
        WHERE e.chunk_id IS NULL AND length(trim(c.text)) > 0
        ORDER BY c.document_id ASC
        """,
        Long.class);
  }

  @Override
  public List<ChunkForEmbedding> findChunksWithoutEmbedding(long documentId) {
    return jdbcTemplate.query(
        """
        SELECT c.id, c.text
        FROM chunk c
        LEFT JOIN embedding e ON e.chunk_id = c.id
        WHERE c.document_id = ?
          AND e.chunk_id IS NULL
          AND length(trim(c.text)) > 0
        ORDER BY c.id ASC
        """,
        (rs, rowNum) -> new ChunkForEmbedding(rs.getLong("id"), rs.getString("text")),
        documentId);
  }

  @Override
  public void upsert(long chunkId, float[] vector, String model) {
    jdbcTemplate.update(
        """
        INSERT INTO embedding (chunk_id, embedding, model, created_at)
        VALUES (?, CAST(? AS vector), ?, NOW())
        ON CONFLICT (chunk_id)
        DO UPDATE SET embedding = EXCLUDED.embedding, model = EXCLUDED.model, created_at = NOW()
        """,
        chunkId,
        toPgVector(vector),
        model);
  }

  private String toPgVector(float[] vector) {
    StringBuilder value = new StringBuilder("[");
    for (int i = 0; i < vector.length; i++) {
      if (!Float.isFinite(vector[i])) {
        throw new IllegalArgumentException("Embedding vector contains a non-finite value");
      }
      if (i > 0) {
        value.append(',');
      }
      value.append(vector[i]);
    }
    return value.append(']').toString();
  }
}
