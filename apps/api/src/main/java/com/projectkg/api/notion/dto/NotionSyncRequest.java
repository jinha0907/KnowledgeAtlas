package com.projectkg.api.notion.dto;

import java.util.List;

public record NotionSyncRequest(
    String sourceType,
    String sourceId,
    String title,
    String rawJson,
    List<NotionBlockDto> blocks
) {}
