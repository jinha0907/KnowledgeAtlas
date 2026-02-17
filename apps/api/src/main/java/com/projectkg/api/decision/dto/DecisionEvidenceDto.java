package com.projectkg.api.decision.dto;

public record DecisionEvidenceDto(
    long id,
    long decisionId,
    long documentId,
    String blockId,
    String quote,
    String rationale
) {}
