package com.projectkg.api.embedding.repository;

import com.projectkg.api.embedding.service.EmbeddingIdentity;
import java.util.List;

public interface EmbeddingRepository {
  List<Long> findDocumentIdsWithMissingEmbeddings();

  List<ChunkForEmbedding> findChunksWithoutEmbedding(long documentId);

  void upsert(long chunkId, float[] vector, EmbeddingIdentity identity);

  default List<EmbeddingIdentity> findEmbeddingIdentities() {
    return List.of();
  }

  default void deleteAll() {
    throw new UnsupportedOperationException("Deleting embeddings is not supported");
  }

  record ChunkForEmbedding(long chunkId, String text) {}
}
