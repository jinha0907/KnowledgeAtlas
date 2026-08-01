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

    String p1 = NotionSyncService.buildChecksumPayload(base, base.blocks(), objectMapper);
    String p2 = NotionSyncService.buildChecksumPayload(edited, edited.blocks(), objectMapper);

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

    String p1 = NotionSyncService.buildChecksumPayload(first, first.blocks(), objectMapper);
    String p2 = NotionSyncService.buildChecksumPayload(second, second.blocks(), objectMapper);

    assertEquals(p1, p2);
  }

  @Test
  void checksumPayloadShouldIgnoreRawSnapshotAndBlockUpdateMetadata() {
    NotionSyncRequest first = new NotionSyncRequest(
        "notion",
        "page_123",
        "Kickoff",
        "{\"id\":\"page_123\",\"last_edited_time\":\"2026-07-17T00:00:00Z\"}",
        List.of(new NotionBlockDto("b1", "hello world", "/a", "2026-07-17T00:00:00Z")));
    NotionSyncRequest second = new NotionSyncRequest(
        "notion",
        "page_123",
        "Kickoff",
        "{\"last_edited_time\":\"2026-07-17T00:01:00Z\",\"id\":\"page_123\",\"request_id\":\"volatile\"}",
        List.of(new NotionBlockDto("b1", "hello world", "/a", "2026-07-17T00:01:00Z")));

    String p1 = NotionSyncService.buildChecksumPayload(first, first.blocks(), objectMapper);
    String p2 = NotionSyncService.buildChecksumPayload(second, second.blocks(), objectMapper);

    assertEquals(p1, p2);
  }

  @Test
  void checksumPayloadShouldChangeWhenCanonicalTitleBlockPathOrMembershipChanges() {
    NotionSyncRequest base = new NotionSyncRequest(
        "notion",
        "page_123",
        "Kickoff",
        "{\"id\":\"page_123\"}",
        List.of(new NotionBlockDto("b1", "hello world", "/a", "2026-07-17T00:00:00Z")));
    NotionSyncRequest pathChanged = new NotionSyncRequest(
        "notion",
        "page_123",
        "Kickoff",
        "{\"id\":\"page_123\"}",
        List.of(new NotionBlockDto("b1", "hello world", "/b", "2026-07-17T00:00:00Z")));
    NotionSyncRequest titleChanged = new NotionSyncRequest(
        "notion",
        "page_123",
        "Updated kickoff",
        "{\"id\":\"page_123\"}",
        List.of(new NotionBlockDto("b1", "hello world", "/a", "2026-07-17T00:00:00Z")));
    NotionSyncRequest membershipChanged = new NotionSyncRequest(
        "notion",
        "page_123",
        "Kickoff",
        "{\"id\":\"page_123\"}",
        List.of(
            new NotionBlockDto("b1", "hello world", "/a", "2026-07-17T00:00:00Z"),
            new NotionBlockDto("b2", "another block", "/a", "2026-07-17T00:00:00Z")));

    String basePayload = NotionSyncService.buildChecksumPayload(base, base.blocks(), objectMapper);

    assertNotEquals(basePayload,
        NotionSyncService.buildChecksumPayload(pathChanged, pathChanged.blocks(), objectMapper));
    assertNotEquals(basePayload,
        NotionSyncService.buildChecksumPayload(titleChanged, titleChanged.blocks(), objectMapper));
    assertNotEquals(basePayload,
        NotionSyncService.buildChecksumPayload(membershipChanged, membershipChanged.blocks(), objectMapper));
  }
}
