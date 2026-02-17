package com.projectkg.api.search.repository;

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
}
