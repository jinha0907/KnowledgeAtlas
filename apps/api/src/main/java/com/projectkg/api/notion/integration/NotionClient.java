package com.projectkg.api.notion.integration;

import java.time.Instant;
import java.util.List;

public interface NotionClient {
  PageResult searchPagesUpdatedAfter(Instant since, String startCursor, int pageSize);

  List<NotionBlock> fetchBlockChildren(String pageId);

  record PageResult(List<NotionPage> pages, boolean hasMore, String nextCursor) {}

  record NotionPage(String id, String title, Instant lastEditedTime, String rawJson) {}

  record NotionBlock(String blockId, String text, String path, Instant updatedAt, String rawJson) {}
}
