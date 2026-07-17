package com.projectkg.api.decision.dto;

import java.util.List;

public record DecisionExtractionResponse(
    String status,
    Long runId,
    int extractedDecisions,
    List<DecisionDto> decisions
) {}
