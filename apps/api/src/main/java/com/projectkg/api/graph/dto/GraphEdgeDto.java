package com.projectkg.api.graph.dto;

public record GraphEdgeDto(
    String id,
    String sourceId,
    String targetId,
    String type,
    long evidenceId,
    String blockId
) {}
