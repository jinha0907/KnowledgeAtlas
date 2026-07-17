package com.projectkg.api.notion.service;

import com.projectkg.api.notion.dto.NotionBlockDto;
import com.projectkg.api.notion.dto.NotionSyncRequest;
import com.projectkg.api.notion.dto.NotionSyncResponse;
import com.projectkg.api.notion.dto.NotionSyncRunRequest;
import com.projectkg.api.notion.dto.NotionSyncRunResponse;
import com.projectkg.api.notion.integration.NotionClient;
import com.projectkg.api.notion.integration.NotionClient.NotionBlock;
import com.projectkg.api.notion.integration.NotionClient.NotionPage;
import com.projectkg.api.notion.integration.NotionClient.PageResult;
import com.projectkg.api.notion.repository.SyncJobRunRepository;
import com.projectkg.api.notion.repository.JdbcSyncJobRunRepository.SyncAlreadyRunningException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

@Service
public class NotionSyncRunnerService {
  private static final String SOURCE_TYPE = "notion";
  private static final int DEFAULT_PAGE_SIZE = 20;
  private static final int MAX_PAGE_SIZE = 100;
  private static final int DEFAULT_MAX_PAGES = 20;
  private static final int MAX_MAX_PAGES = 100;
  private static final Duration SOURCE_OVERLAP = Duration.ofMinutes(2);

  private final NotionClient notionClient;
  private final NotionDocumentSyncService notionSyncService;
  private final SyncJobRunRepository syncJobRunRepository;

  public NotionSyncRunnerService(
      NotionClient notionClient,
      NotionDocumentSyncService notionSyncService,
      SyncJobRunRepository syncJobRunRepository
  ) {
    this.notionClient = notionClient;
    this.notionSyncService = notionSyncService;
    this.syncJobRunRepository = syncJobRunRepository;
  }

  public NotionSyncRunResponse runIncrementalSync(NotionSyncRunRequest request) {
    int pageSize = sanitizePageSize(request == null ? null : request.pageSize());
    int maxPages = sanitizeMaxPages(request == null ? null : request.maxPages());

    Instant previousWatermark = syncJobRunRepository
        .findLatestSuccessfulSourceWatermark(SOURCE_TYPE)
        .orElse(null);
    Instant since = previousWatermark == null ? null : previousWatermark.minus(SOURCE_OVERLAP);
    long jobRunId = syncJobRunRepository.createRunning(SOURCE_TYPE);

    int syncedDocuments = 0;
    int changedDocuments = 0;
    int upsertedBlocks = 0;
    int upsertedChunks = 0;
    Instant latestObservedSourceUpdate = previousWatermark;

    try {
      String cursor = null;
      int pageRounds = 0;

      while (pageRounds < maxPages) {
        final String requestCursor = cursor;
        PageResult pageResult = withRetry(
            () -> notionClient.searchPagesUpdatedAfter(since, requestCursor, pageSize));

        for (NotionPage page : pageResult.pages()) {
          if (latestObservedSourceUpdate == null
              || page.lastEditedTime().isAfter(latestObservedSourceUpdate)) {
            latestObservedSourceUpdate = page.lastEditedTime();
          }

          List<NotionBlock> blocks = withRetry(() -> notionClient.fetchBlockChildren(page.id()));
          NotionSyncRequest syncRequest = toSyncRequest(page, blocks);
          NotionSyncResponse syncResponse = notionSyncService.sync(syncRequest);

          syncedDocuments++;
          if (syncResponse.checksumChanged()) {
            changedDocuments++;
            upsertedBlocks += syncResponse.upsertedBlocks();
            upsertedChunks += syncResponse.upsertedChunks();
          }
        }

        pageRounds++;
        boolean hasNextPage = pageResult.hasMore()
            && pageResult.nextCursor() != null
            && !pageResult.nextCursor().isBlank();
        if (!hasNextPage) {
          break;
        }
        if (pageRounds == maxPages) {
          throw new IllegalStateException(
              "Notion sync reached maxPages before completing the result set; increase maxPages and retry");
        }
        cursor = pageResult.nextCursor();
      }

      Instant nextWatermark = latestObservedSourceUpdate == null ? Instant.now() : latestObservedSourceUpdate;
      syncJobRunRepository.markSuccess(jobRunId, syncedDocuments, nextWatermark);
      return new NotionSyncRunResponse(
          "success",
          jobRunId,
          since == null ? null : since.toString(),
          syncedDocuments,
          changedDocuments,
          upsertedBlocks,
          upsertedChunks);
    } catch (SyncAlreadyRunningException ex) {
      throw ex;
    } catch (Exception ex) {
      syncJobRunRepository.markFailed(jobRunId, ex.getMessage());
      throw new IllegalStateException("Notion incremental sync failed", ex);
    }
  }

  private NotionSyncRequest toSyncRequest(NotionPage page, List<NotionBlock> blocks) {
    List<NotionBlockDto> blockDtos = new ArrayList<>();
    for (NotionBlock block : blocks) {
      blockDtos.add(new NotionBlockDto(
          block.blockId(),
          block.text(),
          block.path(),
          block.updatedAt() == null ? null : block.updatedAt().toString()));
    }

    return new NotionSyncRequest(
        SOURCE_TYPE,
        page.id(),
        page.title(),
        buildSnapshotJson(page, blocks),
        blockDtos);
  }

  private String buildSnapshotJson(NotionPage page, List<NotionBlock> blocks) {
    StringBuilder snapshot = new StringBuilder("{\"page\":");
    snapshot.append(page.rawJson()).append(",\"blocks\":[");
    for (int i = 0; i < blocks.size(); i++) {
      if (i > 0) {
        snapshot.append(',');
      }
      snapshot.append(blocks.get(i).rawJson());
    }
    snapshot.append("]}");
    return snapshot.toString();
  }

  private int sanitizePageSize(Integer pageSize) {
    if (pageSize == null || pageSize <= 0) {
      return DEFAULT_PAGE_SIZE;
    }
    return Math.min(pageSize, MAX_PAGE_SIZE);
  }

  private int sanitizeMaxPages(Integer maxPages) {
    if (maxPages == null || maxPages <= 0) {
      return DEFAULT_MAX_PAGES;
    }
    return Math.min(maxPages, MAX_MAX_PAGES);
  }

  private <T> T withRetry(RetrySupplier<T> supplier) {
    final int maxAttempts = 3;
    long backoffMillis = 300L;

    RuntimeException last = null;
    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      try {
        return supplier.get();
      } catch (RuntimeException ex) {
        last = ex;
        if (attempt == maxAttempts || !shouldRetry(ex)) {
          break;
        }
        sleep(backoffMillis);
        backoffMillis = Math.min(Duration.ofSeconds(2).toMillis(), backoffMillis * 2);
      }
    }

    throw last == null ? new IllegalStateException("retry failed") : last;
  }

  private boolean shouldRetry(RuntimeException ex) {
    if (ex instanceof ResourceAccessException) {
      return true;
    }
    if (ex instanceof RestClientResponseException responseException) {
      int status = responseException.getStatusCode().value();
      return status == 429 || status >= 500;
    }
    return false;
  }

  private void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted during retry backoff", ex);
    }
  }

  @FunctionalInterface
  private interface RetrySupplier<T> {
    T get();
  }
}
