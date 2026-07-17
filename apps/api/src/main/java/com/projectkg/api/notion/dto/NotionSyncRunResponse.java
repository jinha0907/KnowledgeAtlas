package com.projectkg.api.notion.dto;

public record NotionSyncRunResponse(
    String status,
    long jobRunId,
    String since,
    int syncedDocuments,
    int changedDocuments,
    int upsertedBlocks,
    int upsertedChunks
) {}
