package com.projectkg.api.notion.dto;

public record NotionBlockDto(
    String blockId,
    String text,
    String path,
    String updatedAt
) {}
