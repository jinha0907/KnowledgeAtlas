package com.projectkg.api.decision.dto;

public record UpdateDecisionStatusRequest(
    String status,
    Long supersedesDecisionId
) {}
