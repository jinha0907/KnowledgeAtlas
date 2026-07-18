package com.projectkg.api.embedding.service;

public class EmbeddingReindexRequiredException extends RuntimeException {
  public EmbeddingReindexRequiredException(EmbeddingIdentity active, String persisted) {
    super("Embedding configuration differs from persisted vectors. Run POST /api/embeddings/reindex "
        + "with {\"confirm\":true} before using " + active.provider() + "/" + active.model()
        + "/" + active.dimensions() + ". Persisted: " + persisted);
  }
}
