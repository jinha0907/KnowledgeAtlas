package com.projectkg.api.decision.repository;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DecisionEvidenceRepository {
  private final JdbcTemplate jdbcTemplate;

  public DecisionEvidenceRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public long create(long decisionId, long documentId, String blockId, String quote, String rationale) {
    String sql = """
        INSERT INTO decision_evidence (decision_id, document_id, block_id, quote, rationale, created_at)
        VALUES (?, ?, ?, ?, ?, NOW())
        RETURNING id
        """;

    Long id = jdbcTemplate.queryForObject(sql, Long.class, decisionId, documentId, blockId, quote, rationale);
    if (id == null) {
      throw new IllegalStateException("Failed to create decision evidence");
    }
    return id;
  }

  public List<DecisionEvidenceRow> findByDecisionId(long decisionId) {
    String sql = """
        SELECT id, decision_id, document_id, block_id, quote, rationale
        FROM decision_evidence
        WHERE decision_id = ?
        ORDER BY id ASC
        """;

    return jdbcTemplate.query(sql, (rs, rowNum) -> new DecisionEvidenceRow(
            rs.getLong("id"),
            rs.getLong("decision_id"),
            rs.getLong("document_id"),
            rs.getString("block_id"),
            rs.getString("quote"),
            rs.getString("rationale")),
        decisionId);
  }

  public record DecisionEvidenceRow(
      long id,
      long decisionId,
      long documentId,
      String blockId,
      String quote,
      String rationale
  ) {}
}
