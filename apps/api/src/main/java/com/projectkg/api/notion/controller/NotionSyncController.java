package com.projectkg.api.notion.controller;

import com.projectkg.api.notion.dto.NotionSyncRequest;
import com.projectkg.api.notion.dto.NotionSyncResponse;
import com.projectkg.api.notion.dto.NotionSyncRunRequest;
import com.projectkg.api.notion.dto.NotionSyncRunResponse;
import com.projectkg.api.notion.service.NotionSyncRunnerService;
import com.projectkg.api.notion.service.NotionSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notion")
@Tag(name = "notion", description = "Notion ingest and sync runner APIs")
public class NotionSyncController {
  private final NotionSyncService notionSyncService;
  private final NotionSyncRunnerService notionSyncRunnerService;

  public NotionSyncController(
      NotionSyncService notionSyncService,
      NotionSyncRunnerService notionSyncRunnerService
  ) {
    this.notionSyncService = notionSyncService;
    this.notionSyncRunnerService = notionSyncRunnerService;
  }

  @PostMapping("/sync")
  @Operation(summary = "Sync one normalized Notion payload into DB")
  public ResponseEntity<NotionSyncResponse> sync(@RequestBody NotionSyncRequest request) {
    return ResponseEntity.ok(notionSyncService.sync(request));
  }

  @PostMapping("/sync/run")
  @Operation(summary = "Run real incremental sync from Notion API")
  public ResponseEntity<NotionSyncRunResponse> runSync(@RequestBody(required = false) NotionSyncRunRequest request) {
    return ResponseEntity.ok(notionSyncRunnerService.runIncrementalSync(request));
  }
}
