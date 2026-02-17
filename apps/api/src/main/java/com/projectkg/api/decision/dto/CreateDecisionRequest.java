package com.projectkg.api.decision.dto;

public record CreateDecisionRequest(
    String title,
    String outcome,
    String status,
    Long supersedesDecisionId
) {}
