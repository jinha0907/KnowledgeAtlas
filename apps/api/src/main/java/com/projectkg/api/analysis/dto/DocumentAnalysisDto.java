package com.projectkg.api.analysis.dto;

import java.time.Instant;
import java.util.List;

public record DocumentAnalysisDto(
    long runId,
    String status,
    String summary,
    List<String> tags,
    Instant completedAt
) {}
