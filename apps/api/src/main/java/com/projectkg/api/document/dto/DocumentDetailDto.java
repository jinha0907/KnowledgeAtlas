package com.projectkg.api.document.dto;

import java.time.Instant;
import java.util.List;

public record DocumentDetailDto(
    long id,
    String title,
    Instant lastSyncedAt,
    List<DocumentBlockDto> blocks
) {}
