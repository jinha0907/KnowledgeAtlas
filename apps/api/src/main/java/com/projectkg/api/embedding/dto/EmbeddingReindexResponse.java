package com.projectkg.api.embedding.dto;

public record EmbeddingReindexResponse(String status, int documents, int embeddings) {}
