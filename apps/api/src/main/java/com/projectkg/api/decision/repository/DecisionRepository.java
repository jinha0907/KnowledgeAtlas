package com.projectkg.api.decision.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DecisionRepository {
  private final JdbcTemplate jdbcTemplate;

  public DecisionRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public long create(String title, String status, String outcome, Long supersedesDecisionId) {
    String sql = """
        INSERT INTO decision (title, status, outcome, supersedes_decision_id, created_at, updated_at)
        VALUES (?, ?, ?, ?, NOW(), NOW())
        RETURNING id
        """;

    Long id = jdbcTemplate.queryForObject(sql, Long.class, title, status, outcome, supersedesDecisionId);
    if (id == null) {
      throw new IllegalStateException("Failed to create decision");
    }
    return id;
  }

  public long createExtracted(
      String title,
      String discussion,
      String outcome,
      double confidence,
      long extractionRunId
  ) {
    String sql = """
        INSERT INTO decision (
          title, status, discussion, outcome, confidence, extraction_run_id, created_at, updated_at
        )
        VALUES (?, 'proposed', ?, ?, ?, ?, NOW(), NOW())
        RETURNING id
        """;

    Long id = jdbcTemplate.queryForObject(
        sql, Long.class, title, discussion, outcome, confidence, extractionRunId);
    if (id == null) {
      throw new IllegalStateException("Failed to create extracted decision");
    }
    return id;
  }

  public Optional<DecisionRow> findById(long id) {
    String sql = """
        SELECT id, title, status, discussion, outcome, confidence, supersedes_decision_id, extraction_run_id,
               created_at, updated_at
        FROM decision
        WHERE id = ?
        """;

    return jdbcTemplate.query(sql, (rs, rowNum) -> new DecisionRow(
            rs.getLong("id"),
            rs.getString("title"),
            rs.getString("status"),
            rs.getString("discussion"),
            rs.getString("outcome"),
            (Double) rs.getObject("confidence"),
            (Long) rs.getObject("supersedes_decision_id"),
            (Long) rs.getObject("extraction_run_id"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant()),
        id).stream().findFirst();
  }

  public List<DecisionRow> findAll() {
    String sql = """
        SELECT id, title, status, discussion, outcome, confidence, supersedes_decision_id, extraction_run_id,
               created_at, updated_at
        FROM decision
        ORDER BY updated_at DESC, id DESC
        """;

    return jdbcTemplate.query(sql, (rs, rowNum) -> new DecisionRow(
        rs.getLong("id"),
        rs.getString("title"),
        rs.getString("status"),
        rs.getString("discussion"),
        rs.getString("outcome"),
        (Double) rs.getObject("confidence"),
        (Long) rs.getObject("supersedes_decision_id"),
        (Long) rs.getObject("extraction_run_id"),
        rs.getTimestamp("created_at").toInstant(),
        rs.getTimestamp("updated_at").toInstant()));
  }

  public void updateStatus(long id, String status, Long supersedesDecisionId, Instant updatedAt) {
    String sql = """
        UPDATE decision
        SET status = ?, supersedes_decision_id = ?, updated_at = ?
        WHERE id = ?
        """;

    jdbcTemplate.update(sql, status, supersedesDecisionId, Timestamp.from(updatedAt), id);
  }

  public List<DecisionRow> findByExtractionRunId(long extractionRunId) {
    String sql = """
        SELECT id, title, status, discussion, outcome, confidence, supersedes_decision_id, extraction_run_id,
               created_at, updated_at
        FROM decision
        WHERE extraction_run_id = ?
        ORDER BY id ASC
        """;
    return jdbcTemplate.query(sql, (rs, rowNum) -> new DecisionRow(
        rs.getLong("id"),
        rs.getString("title"),
        rs.getString("status"),
        rs.getString("discussion"),
        rs.getString("outcome"),
        (Double) rs.getObject("confidence"),
        (Long) rs.getObject("supersedes_decision_id"),
        (Long) rs.getObject("extraction_run_id"),
        rs.getTimestamp("created_at").toInstant(),
        rs.getTimestamp("updated_at").toInstant()), extractionRunId);
  }

  public record DecisionRow(
      long id,
      String title,
      String status,
      String discussion,
      String outcome,
      Double confidence,
      Long supersedesDecisionId,
      Long extractionRunId,
      Instant createdAt,
      Instant updatedAt
  ) {}
}
