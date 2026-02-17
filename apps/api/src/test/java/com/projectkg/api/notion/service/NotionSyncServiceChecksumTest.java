package com.projectkg.api.notion.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectkg.api.notion.dto.NotionBlockDto;
import com.projectkg.api.notion.dto.NotionSyncRequest;
import java.util.List;
import org.junit.jupiter.api.Test;

class NotionSyncServiceChecksumTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void checksumPayloadShouldChangeWhenBlockTextChanges() {
    NotionSyncRequest base = new NotionSyncRequest(
        "notion",
        "page_123",
        "Kickoff",
        "{\"id\":\"page_123\"}",
        List.of(new NotionBlockDto("b1", "hello world", "/a", "2026-02-16T23:40:00+09:00")));

    NotionSyncRequest edited = new NotionSyncRequest(
        "notion",
        "page_123",
        "Kickoff",
        "{\"id\":\"page_123\"}",
        List.of(new NotionBlockDto("b1", "hello world updated", "/a", "2026-02-16T23:40:00+09:00")));

    String p1 = NotionSyncService.buildChecksumPayload(base, "{\"id\":\"page_123\"}", base.blocks(), objectMapper);
    String p2 = NotionSyncService.buildChecksumPayload(edited, "{\"id\":\"page_123\"}", edited.blocks(), objectMapper);

    assertNotEquals(p1, p2);
  }

  @Test
  void checksumPayloadShouldBeStableWhenBlocksReordered() {
    NotionSyncRequest first = new NotionSyncRequest(
        "notion",
        "page_123",
        "Kickoff",
        "{\"id\":\"page_123\"}",
        List.of(
            new NotionBlockDto("b2", "second", "/a", "2026-02-16T23:41:00+09:00"),
            new NotionBlockDto("b1", "first", "/a", "2026-02-16T23:40:00+09:00")));

    NotionSyncRequest second = new NotionSyncRequest(
        "notion",
        "page_123",
        "Kickoff",
        "{\"id\":\"page_123\"}",
        List.of(
            new NotionBlockDto("b1", "first", "/a", "2026-02-16T23:40:00+09:00"),
            new NotionBlockDto("b2", "second", "/a", "2026-02-16T23:41:00+09:00")));

    String p1 = NotionSyncService.buildChecksumPayload(first, "{\"id\":\"page_123\"}", first.blocks(), objectMapper);
    String p2 = NotionSyncService.buildChecksumPayload(second, "{\"id\":\"page_123\"}", second.blocks(), objectMapper);

    assertEquals(p1, p2);
  }
}
