package com.projectkg.api.notion.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.projectkg.api.notion.dto.NotionBlockDto;
import com.projectkg.api.notion.dto.NotionSyncRequest;
import com.projectkg.api.notion.dto.NotionSyncResponse;
import com.projectkg.api.document.service.DocumentService;
import com.projectkg.api.analysis.repository.DocumentAnalysisRunRepository;
import com.projectkg.api.embedding.service.EmbeddingIdentity;
import com.projectkg.api.graph.service.ProjectGraphService;
import com.projectkg.api.search.repository.JdbcSearchRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class NotionSyncPersistenceIntegrationTest {
  private static final DockerImageName PGVECTOR_IMAGE = DockerImageName
      .parse("pgvector/pgvector:pg16")
      .asCompatibleSubstituteFor("postgres");

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(PGVECTOR_IMAGE)
      .withDatabaseName("projectkg")
      .withUsername("projectkg")
      .withPassword("projectkg");

  @DynamicPropertySource
  static void configureDataSource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Autowired
  private NotionSyncService notionSyncService;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private JdbcSearchRepository jdbcSearchRepository;

  @Autowired
  private DocumentService documentService;

  @Autowired
  private DocumentAnalysisRunRepository documentAnalysisRunRepository;

  @Autowired
  private ProjectGraphService projectGraphService;

  @Test
  void shouldApplyMigrationsAndRemoveBlocksDeletedFromTheSource() {
    NotionSyncResponse firstSync = notionSyncService.sync(new NotionSyncRequest(
        "notion",
        "page-1",
        "Project plan",
        "{\"page\":{\"id\":\"page-1\"},\"blocks\":[\"block-a\",\"block-b\"]}",
        List.of(
            block("block-a", "First project detail"),
            block("block-b", "Deprecated project detail"))));

    assertTrue(firstSync.checksumChanged());
    assertEquals(7, jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM flyway_schema_history WHERE success", Integer.class));
    assertEquals("vector", jdbcTemplate.queryForObject(
        "SELECT extname FROM pg_extension WHERE extname = 'vector'", String.class));
    assertEquals(2, count("content_block"));
    assertEquals(2, count("chunk"));
    assertEquals(1, documentService.list().size());
    assertEquals("Project plan", documentService.getById(firstSync.documentId()).title());
    assertEquals(2, documentService.getById(firstSync.documentId()).blocks().size());
    shouldPersistOneIdempotentDocumentAnalysis(firstSync.documentId());
    long originalChunkId = jdbcTemplate.queryForObject(
        "SELECT id FROM chunk WHERE block_id = 'block-a'", Long.class);
    jdbcTemplate.update(
        """
        INSERT INTO embedding (chunk_id, embedding, provider, model, dimensions)
        VALUES (?, CAST(? AS vector), 'openai', 'test', 1536)
        """,
        originalChunkId,
        unitVector());
    assertTrue(jdbcSearchRepository.searchByHybrid(
        "first", unitVectorValues(), new EmbeddingIdentity("openai", "test", 1536), 5).stream()
        .anyMatch(row -> row.blockId().equals("block-a")));
    shouldPersistOneIdempotentDecisionExtractionRun(firstSync.documentId());
    assertEquals(3, projectGraphService.getGraph().nodes().size());
    assertEquals(List.of("edge-decision-1", "edge-document-1"), projectGraphService.getGraph().edges()
        .stream().map(edge -> edge.id()).toList());

    NotionSyncResponse secondSync = notionSyncService.sync(new NotionSyncRequest(
        "notion",
        "page-1",
        "Project plan",
        "{\"page\":{\"id\":\"page-1\"},\"blocks\":[\"block-a\"]}",
        List.of(block("block-a", "First project detail"))));

    assertTrue(secondSync.checksumChanged());
    assertEquals(1, count("content_block"));
    assertEquals(1, count("chunk"));
    assertEquals(originalChunkId, jdbcTemplate.queryForObject(
        "SELECT id FROM chunk WHERE block_id = 'block-a'", Long.class));
    assertEquals(1, count("embedding"));
    assertEquals(0, jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM chunk WHERE block_id = 'block-b'", Integer.class));

    NotionSyncResponse thirdSync = notionSyncService.sync(new NotionSyncRequest(
        "notion",
        "page-1",
        "Project plan",
        "{\"page\":{\"id\":\"page-1\"},\"blocks\":[\"block-a\"],\"revision\":2}",
        List.of(block("block-a", "Updated project detail"))));

    assertTrue(thirdSync.checksumChanged());
    assertNotEquals(originalChunkId, jdbcTemplate.queryForObject(
        "SELECT id FROM chunk WHERE block_id = 'block-a'", Long.class));
    assertEquals(0, count("embedding"));
  }

  private NotionBlockDto block(String blockId, String text) {
    return new NotionBlockDto(
        blockId,
        text,
        "page:page-1/paragraph:" + blockId,
        "2026-07-17T00:00:00Z");
  }

  private int count(String tableName) {
    return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
  }

  private String unitVector() {
    return "[1," + "0,".repeat(1534) + "0]";
  }

  private void shouldPersistOneIdempotentDecisionExtractionRun(long documentId) {
    long runId = jdbcTemplate.queryForObject(
        """
        INSERT INTO decision_extraction_run (document_id, source_checksum, status)
        VALUES (?, 'document-checksum', 'success')
        RETURNING id
        """,
        Long.class,
        documentId);
    long decisionId = jdbcTemplate.queryForObject(
        """
        INSERT INTO decision (title, status, discussion, outcome, confidence, extraction_run_id)
        VALUES ('Deployment', 'proposed', 'Release timing was discussed', 'Deploy Friday', 0.9, ?)
        RETURNING id
        """,
        Long.class,
        runId);
    jdbcTemplate.update(
        """
        INSERT INTO decision_evidence (decision_id, document_id, block_id, quote, rationale)
        VALUES (?, ?, 'block-a', 'First project detail', 'Explicit decision evidence')
        """,
        decisionId,
        documentId);

    assertEquals(1, jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM decision WHERE extraction_run_id = ?", Integer.class, runId));
    assertEquals(1, jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM decision_evidence WHERE decision_id = ?", Integer.class, decisionId));
    assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
        """
        INSERT INTO decision_extraction_run (document_id, source_checksum, status)
        VALUES (?, 'document-checksum', 'running')
        """,
        documentId));
  }

  private void shouldPersistOneIdempotentDocumentAnalysis(long documentId) {
    String sourceChecksum = jdbcTemplate.queryForObject(
        "SELECT checksum FROM source_document WHERE id = ?", String.class, documentId);
    long runId = documentAnalysisRunRepository.createRunning(documentId, sourceChecksum);
    documentAnalysisRunRepository.markSuccess(
        runId, "Project planning and delivery milestones.", List.of("roadmap", "delivery"));

    DocumentAnalysisRunRepository.AnalysisRunRow run = documentAnalysisRunRepository
        .findByDocumentAndChecksum(documentId, sourceChecksum)
        .orElseThrow();
    assertEquals("success", run.status());
    assertEquals(List.of("roadmap", "delivery"), run.tags());
    assertEquals("Project planning and delivery milestones.",
        documentService.getById(documentId).analysis().summary());
    assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
        """
        INSERT INTO document_analysis_run (document_id, source_checksum, status)
        VALUES (?, ?, 'running')
        """,
        documentId,
        sourceChecksum));
  }

  private float[] unitVectorValues() {
    float[] vector = new float[1536];
    vector[0] = 1.0f;
    return vector;
  }
}
