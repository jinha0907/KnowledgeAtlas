package com.projectkg.api.knowledgegraph.dto;

import com.projectkg.api.embedding.dto.EmbeddingIdentityDto;
import java.util.List;

public record KnowledgeGraphDto(
    String status,
    EmbeddingIdentityDto embeddingIdentity,
    List<KnowledgeGraphNodeDto> nodes,
    List<KnowledgeGraphEdgeDto> edges
) {}
