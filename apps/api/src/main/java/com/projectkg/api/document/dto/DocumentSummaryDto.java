package com.projectkg.api.document.dto;

import com.projectkg.api.analysis.dto.DocumentAnalysisDto;
import java.time.Instant;

public record DocumentSummaryDto(
    long id,
    String sourceType,
    String sourceId,
    String title,
    Instant lastSyncedAt,
    DocumentAnalysisDto analysis
) {}
