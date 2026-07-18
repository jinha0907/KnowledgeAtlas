package com.projectkg.api.document.dto;

import com.projectkg.api.analysis.dto.DocumentAnalysisDto;
import java.time.Instant;
import java.util.List;

public record DocumentDetailDto(
    long id,
    String title,
    Instant lastSyncedAt,
    List<DocumentBlockDto> blocks,
    DocumentAnalysisDto analysis
) {}
