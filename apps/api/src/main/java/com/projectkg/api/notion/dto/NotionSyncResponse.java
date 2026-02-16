package com.projectkg.api.notion.dto;

public record NotionSyncResponse(
    String status,
    long documentId,
    int upsertedBlocks,
    int upsertedChunks,
    boolean checksumChanged
) {}
