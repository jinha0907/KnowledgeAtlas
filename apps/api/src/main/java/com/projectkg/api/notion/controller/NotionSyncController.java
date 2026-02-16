package com.projectkg.api.notion.controller;

import com.projectkg.api.notion.dto.NotionSyncRequest;
import com.projectkg.api.notion.dto.NotionSyncResponse;
import com.projectkg.api.notion.service.NotionSyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notion")
public class NotionSyncController {
  private final NotionSyncService notionSyncService;

  public NotionSyncController(NotionSyncService notionSyncService) {
    this.notionSyncService = notionSyncService;
  }

  @PostMapping("/sync")
  public ResponseEntity<NotionSyncResponse> sync(@RequestBody NotionSyncRequest request) {
    return ResponseEntity.ok(notionSyncService.sync(request));
  }
}
