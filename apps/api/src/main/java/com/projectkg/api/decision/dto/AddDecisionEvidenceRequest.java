package com.projectkg.api.decision.dto;

public record AddDecisionEvidenceRequest(
    long documentId,
    String blockId,
    String quote,
    String rationale
) {}
