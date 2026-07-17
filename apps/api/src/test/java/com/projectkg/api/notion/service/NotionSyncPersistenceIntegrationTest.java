package com.projectkg.api.notion.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.projectkg.api.notion.dto.NotionBlockDto;
import com.projectkg.api.notion.dto.NotionSyncRequest;
import com.projectkg.api.notion.dto.NotionSyncResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
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
    assertEquals(4, jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM flyway_schema_history WHERE success", Integer.class));
    assertEquals("vector", jdbcTemplate.queryForObject(
        "SELECT extname FROM pg_extension WHERE extname = 'vector'", String.class));
    assertEquals(2, count("content_block"));
    assertEquals(2, count("chunk"));

    NotionSyncResponse secondSync = notionSyncService.sync(new NotionSyncRequest(
        "notion",
        "page-1",
        "Project plan",
        "{\"page\":{\"id\":\"page-1\"},\"blocks\":[\"block-a\"]}",
        List.of(block("block-a", "First project detail"))));

    assertTrue(secondSync.checksumChanged());
    assertEquals(1, count("content_block"));
    assertEquals(1, count("chunk"));
    assertEquals(0, jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM chunk WHERE block_id = 'block-b'", Integer.class));
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
}
