package com.projectkg.api.graph.repository;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProjectGraphRepository {
  private final JdbcTemplate jdbcTemplate;

  public ProjectGraphRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public List<DocumentRow> findDocuments() {
    return jdbcTemplate.query(
        """
        SELECT id, title
        FROM source_document
        ORDER BY id ASC
        """,
        (rs, rowNum) -> new DocumentRow(rs.getLong("id"), rs.getString("title")));
  }

  public List<DecisionRow> findDecisions() {
    return jdbcTemplate.query(
        """
        SELECT id, title, status
        FROM decision
        ORDER BY id ASC
        """,
        (rs, rowNum) -> new DecisionRow(
            rs.getLong("id"), rs.getString("title"), rs.getString("status")));
  }

  public List<EvidenceRow> findEvidence() {
    return jdbcTemplate.query(
        """
        SELECT id, decision_id, document_id, block_id, quote
        FROM decision_evidence
        ORDER BY id ASC
        """,
        (rs, rowNum) -> new EvidenceRow(
            rs.getLong("id"),
            rs.getLong("decision_id"),
            rs.getLong("document_id"),
            rs.getString("block_id"),
            rs.getString("quote")));
  }

  public record DocumentRow(long id, String title) {}

  public record DecisionRow(long id, String title, String status) {}

  public record EvidenceRow(long id, long decisionId, long documentId, String blockId, String quote) {}
}
