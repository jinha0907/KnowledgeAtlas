package com.projectkg.api.embedding.repository;

import java.util.List;

public interface EmbeddingRepository {
  List<Long> findDocumentIdsWithMissingEmbeddings();

  List<ChunkForEmbedding> findChunksWithoutEmbedding(long documentId);

  void upsert(long chunkId, float[] vector, String model);

  record ChunkForEmbedding(long chunkId, String text) {}
}
