package com.projectkg.api.embedding.service;

public record EmbeddingIdentity(String provider, String model, int dimensions) {
  public EmbeddingIdentity {
    if (provider == null || provider.isBlank() || model == null || model.isBlank() || dimensions <= 0) {
      throw new IllegalArgumentException("Embedding identity requires provider, model, and positive dimensions");
    }
  }
}
