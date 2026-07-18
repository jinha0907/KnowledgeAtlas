package com.projectkg.api.graph.dto;

public record GraphNodeDto(
    String id,
    String type,
    String label,
    String status,
    Long documentId,
    Long decisionId,
    Long evidenceId,
    String blockId
) {}
