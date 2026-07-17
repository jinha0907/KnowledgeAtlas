package com.projectkg.api.document.dto;

import java.time.Instant;

public record DocumentSummaryDto(
    long id,
    String sourceType,
    String sourceId,
    String title,
    Instant lastSyncedAt
) {}
