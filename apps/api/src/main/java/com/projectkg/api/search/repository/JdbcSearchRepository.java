package com.projectkg.api.search.repository;

import com.projectkg.api.embedding.service.EmbeddingIdentity;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcSearchRepository implements SearchRepository {
  private final JdbcTemplate jdbcTemplate;

  public JdbcSearchRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public List<SearchRow> searchByKeyword(String query, int topK) {
    String sql = """
        SELECT
          c.id AS chunk_id,
          c.document_id,
          c.block_id,
          sd.title,
          c.text,
          ts_rank_cd(c.search_vector, websearch_to_tsquery('simple', ?)) AS score
        FROM chunk c
        JOIN source_document sd ON sd.id = c.document_id
        WHERE c.search_vector @@ websearch_to_tsquery('simple', ?)
        ORDER BY score DESC, c.id ASC
        LIMIT ?
        """;

    return jdbcTemplate.query(
        sql,
        (rs, rowNum) -> new SearchRow(
            rs.getLong("chunk_id"),
            rs.getLong("document_id"),
            rs.getString("block_id"),
            rs.getString("title"),
            rs.getString("text"),
            rs.getDouble("score")),
        query,
        query,
        topK);
  }

  @Override
  public List<SearchRow> searchByHybrid(
      String query, float[] queryEmbedding, EmbeddingIdentity identity, int topK
  ) {
    int candidateLimit = Math.max(topK * 4, 20);
    String sql = """
        WITH keyword_candidates AS (
          SELECT c.id AS chunk_id,
                 1.0 / (60 + ROW_NUMBER() OVER (
                   ORDER BY ts_rank_cd(c.search_vector, websearch_to_tsquery('simple', ?)) DESC, c.id ASC
                 )) AS rrf_score
          FROM chunk c
          WHERE c.search_vector @@ websearch_to_tsquery('simple', ?)
          ORDER BY rrf_score DESC, c.id ASC
          LIMIT ?
        ),
        vector_candidates AS (
          SELECT e.chunk_id,
                 1.0 / (60 + ROW_NUMBER() OVER (ORDER BY e.embedding <=> CAST(? AS vector), e.chunk_id ASC))
                   AS rrf_score
          FROM embedding e
          WHERE e.provider = ? AND e.model = ? AND e.dimensions = ?
          ORDER BY e.embedding <=> CAST(? AS vector), e.chunk_id ASC
          LIMIT ?
        ),
        fused AS (
          SELECT chunk_id, SUM(rrf_score) AS score
          FROM (
            SELECT chunk_id, rrf_score FROM keyword_candidates
            UNION ALL
            SELECT chunk_id, rrf_score FROM vector_candidates
          ) candidates
          GROUP BY chunk_id
        )
        SELECT f.chunk_id, c.document_id, c.block_id, sd.title, c.text, f.score
        FROM fused f
        JOIN chunk c ON c.id = f.chunk_id
        JOIN source_document sd ON sd.id = c.document_id
        ORDER BY f.score DESC, f.chunk_id ASC
        LIMIT ?
        """;

    String vector = toPgVector(queryEmbedding);
    return jdbcTemplate.query(
        sql,
        (rs, rowNum) -> new SearchRow(
            rs.getLong("chunk_id"),
            rs.getLong("document_id"),
            rs.getString("block_id"),
            rs.getString("title"),
            rs.getString("text"),
            rs.getDouble("score")),
        query,
        query,
        candidateLimit,
        vector,
        identity.provider(),
        identity.model(),
        identity.dimensions(),
        vector,
        candidateLimit,
        topK);
  }

  @Override
  public List<EmbeddingIdentity> findEmbeddingIdentities() {
    return jdbcTemplate.query(
        """
        SELECT provider, model, dimensions
        FROM embedding
        GROUP BY provider, model, dimensions
        ORDER BY provider ASC, model ASC, dimensions ASC
        """,
        (rs, rowNum) -> new EmbeddingIdentity(
            rs.getString("provider"), rs.getString("model"), rs.getInt("dimensions")));
  }

  private String toPgVector(float[] vector) {
    StringBuilder value = new StringBuilder("[");
    for (int i = 0; i < vector.length; i++) {
      if (!Float.isFinite(vector[i])) {
        throw new IllegalArgumentException("Query embedding contains a non-finite value");
      }
      if (i > 0) {
        value.append(',');
      }
      value.append(vector[i]);
    }
    return value.append(']').toString();
  }
}
