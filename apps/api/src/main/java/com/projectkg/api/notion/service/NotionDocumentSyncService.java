package com.projectkg.api.notion.service;

import com.projectkg.api.notion.dto.NotionSyncRequest;
import com.projectkg.api.notion.dto.NotionSyncResponse;

public interface NotionDocumentSyncService {
  NotionSyncResponse sync(NotionSyncRequest request);
}
