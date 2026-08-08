package com.projectkg.api.knowledgegraph.dto;

import java.util.List;

public record KnowledgeGraphNodeDto(
    String id,
    long documentId,
    String label,
    String summary,
    List<String> tags,
    int blockCount
) {}
