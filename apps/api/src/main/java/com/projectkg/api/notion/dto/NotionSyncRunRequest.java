package com.projectkg.api.notion.dto;

public record NotionSyncRunRequest(
    Integer pageSize,
    Integer maxPages
) {}
