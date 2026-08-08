package com.projectkg.api.knowledgegraph.dto;

public record KnowledgeGraphEdgeDto(
    String id,
    long sourceDocumentId,
    long targetDocumentId,
    double score,
    KnowledgeGraphCitationDto sourceCitation,
    KnowledgeGraphCitationDto targetCitation
) {}
