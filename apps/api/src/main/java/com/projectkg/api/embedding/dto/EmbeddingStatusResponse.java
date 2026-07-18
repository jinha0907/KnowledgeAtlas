package com.projectkg.api.embedding.dto;

import java.util.List;

public record EmbeddingStatusResponse(
    String status,
    EmbeddingIdentityDto activeIdentity,
    List<EmbeddingIdentityDto> persistedIdentities,
    long eligibleChunks,
    long embeddedChunks,
    long missingChunks,
    boolean reindexRequired
) {}
