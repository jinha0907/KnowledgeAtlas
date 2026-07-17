package com.projectkg.api.notion.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.projectkg.api.notion.dto.NotionSyncRequest;
import com.projectkg.api.notion.dto.NotionSyncResponse;
import com.projectkg.api.notion.dto.NotionSyncRunResponse;
import com.projectkg.api.notion.integration.NotionClient;
import com.projectkg.api.notion.integration.NotionClient.NotionBlock;
import com.projectkg.api.notion.integration.NotionClient.NotionPage;
import com.projectkg.api.notion.integration.NotionClient.PageResult;
import com.projectkg.api.notion.repository.SyncJobRunRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class NotionSyncRunnerServiceTest {

  @Test
  void shouldRunIncrementalSyncAndStoreMaximumSourceWatermark() {
    FakeSyncJobRunRepository jobRuns = new FakeSyncJobRunRepository(Optional.empty());
    Instant editedAt = Instant.parse("2026-07-17T00:00:00Z");
    FakeNotionClient notionClient = new FakeNotionClient(new PageResult(List.of(
        new NotionPage("page1", "Title", editedAt, "{\"id\":\"page1\"}")
    ), false, null));
    FakeDocumentSyncService documentSync = new FakeDocumentSyncService(true);

    NotionSyncRunnerService service = new NotionSyncRunnerService(notionClient, documentSync, jobRuns);
    NotionSyncRunResponse response = service.runIncrementalSync(null);

    assertEquals("success", response.status());
    assertEquals(1, response.syncedDocuments());
    assertEquals(1, response.changedDocuments());
    assertEquals(editedAt, jobRuns.successWatermark);
    assertEquals(1, jobRuns.successSyncedDocuments);
  }

  @Test
  void shouldUseAnOverlapBeforeThePreviousWatermark() {
    Instant previousWatermark = Instant.parse("2026-07-17T00:05:00Z");
    FakeSyncJobRunRepository jobRuns = new FakeSyncJobRunRepository(Optional.of(previousWatermark));
    FakeNotionClient notionClient = new FakeNotionClient(new PageResult(List.of(), false, null));
    NotionSyncRunnerService service = new NotionSyncRunnerService(
        notionClient,
        new FakeDocumentSyncService(false),
        jobRuns);

    service.runIncrementalSync(null);

    assertEquals(previousWatermark.minus(Duration.ofMinutes(2)), notionClient.requestedSince);
    assertEquals(previousWatermark, jobRuns.successWatermark);
  }

  @Test
  void shouldPersistFailureWhenTheNotionCallFails() {
    FakeSyncJobRunRepository jobRuns = new FakeSyncJobRunRepository(Optional.empty());
    FakeNotionClient notionClient = new FakeNotionClient(new IllegalStateException("notion unavailable"));
    NotionSyncRunnerService service = new NotionSyncRunnerService(
        notionClient,
        new FakeDocumentSyncService(false),
        jobRuns);

    assertThrows(IllegalStateException.class, () -> service.runIncrementalSync(null));

    assertEquals("notion unavailable", jobRuns.failureMessage);
  }

  @Test
  void shouldFailWithoutAdvancingTheWatermarkWhenResultSetIsTruncated() {
    FakeSyncJobRunRepository jobRuns = new FakeSyncJobRunRepository(Optional.empty());
    FakeNotionClient notionClient = new FakeNotionClient(new PageResult(List.of(), true, "next-page"));
    NotionSyncRunnerService service = new NotionSyncRunnerService(
        notionClient,
        new FakeDocumentSyncService(false),
        jobRuns);

    assertThrows(IllegalStateException.class,
        () -> service.runIncrementalSync(new com.projectkg.api.notion.dto.NotionSyncRunRequest(1, 1)));

    assertEquals(null, jobRuns.successWatermark);
    assertEquals(
        "Notion sync reached maxPages before completing the result set; increase maxPages and retry",
        jobRuns.failureMessage);
  }

  private static final class FakeNotionClient implements NotionClient {
    private final PageResult pageResult;
    private final RuntimeException failure;
    private Instant requestedSince;

    private FakeNotionClient(PageResult pageResult) {
      this.pageResult = pageResult;
      this.failure = null;
    }

    private FakeNotionClient(RuntimeException failure) {
      this.pageResult = null;
      this.failure = failure;
    }

    @Override
    public PageResult searchPagesUpdatedAfter(Instant since, String startCursor, int pageSize) {
      requestedSince = since;
      if (failure != null) {
        throw failure;
      }
      return pageResult;
    }

    @Override
    public List<NotionBlock> fetchBlockChildren(String pageId) {
      return List.of(new NotionBlock(
          "block1",
          "hello",
          "page:" + pageId + "/paragraph:block1",
          Instant.parse("2026-07-17T00:00:00Z"),
          "{\"id\":\"block1\"}"));
    }
  }

  private static final class FakeDocumentSyncService implements NotionDocumentSyncService {
    private final boolean changed;

    private FakeDocumentSyncService(boolean changed) {
      this.changed = changed;
    }

    @Override
    public NotionSyncResponse sync(NotionSyncRequest request) {
      return new NotionSyncResponse("ok", 1L, changed ? 1 : 0, changed ? 1 : 0, changed);
    }
  }

  private static final class FakeSyncJobRunRepository implements SyncJobRunRepository {
    private final Optional<Instant> priorWatermark;
    private int successSyncedDocuments;
    private Instant successWatermark;
    private String failureMessage;

    private FakeSyncJobRunRepository(Optional<Instant> priorWatermark) {
      this.priorWatermark = priorWatermark;
    }

    @Override
    public long createRunning(String sourceType) {
      return 101L;
    }

    @Override
    public void markSuccess(long id, int syncedDocuments, Instant sourceWatermarkAt) {
      successSyncedDocuments = syncedDocuments;
      successWatermark = sourceWatermarkAt;
    }

    @Override
    public void markFailed(long id, String errorMessage) {
      failureMessage = errorMessage;
    }

    @Override
    public Optional<Instant> findLatestSuccessfulSourceWatermark(String sourceType) {
      return priorWatermark;
    }
  }
}
