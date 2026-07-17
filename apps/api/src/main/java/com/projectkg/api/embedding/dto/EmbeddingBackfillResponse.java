package com.projectkg.api.embedding.dto;

public record EmbeddingBackfillResponse(
    String status,
    int documents,
    int embeddings
) {}
