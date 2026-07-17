package com.projectkg.api.decision.dto;

import java.time.Instant;
import java.util.List;

public record DecisionDto(
    long id,
    String title,
    String status,
    String discussion,
    String outcome,
    Double confidence,
    Long supersedesDecisionId,
    Long extractionRunId,
    Instant createdAt,
    Instant updatedAt,
    List<DecisionEvidenceDto> evidence
) {}
