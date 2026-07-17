package com.projectkg.api.embedding.service;

import com.projectkg.api.embedding.repository.EmbeddingRepository;
import com.projectkg.api.embedding.repository.EmbeddingRepository.ChunkForEmbedding;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class EmbeddingBackfillService {
  public static final int EMBEDDING_DIMENSIONS = 1536;
  private static final int BATCH_SIZE = 100;

  private final EmbeddingRepository embeddingRepository;
  private final Optional<EmbeddingProvider> embeddingProvider;

  public EmbeddingBackfillService(
      EmbeddingRepository embeddingRepository,
      Optional<EmbeddingProvider> embeddingProvider
  ) {
    this.embeddingRepository = embeddingRepository;
    this.embeddingProvider = embeddingProvider;
  }

  public int backfillDocument(long documentId) {
    if (embeddingProvider.isEmpty()) {
      return 0;
    }

    List<ChunkForEmbedding> chunks = embeddingRepository.findChunksWithoutEmbedding(documentId);
    int embedded = 0;
    for (int start = 0; start < chunks.size(); start += BATCH_SIZE) {
      List<ChunkForEmbedding> batch = chunks.subList(start, Math.min(start + BATCH_SIZE, chunks.size()));
      List<String> inputs = batch.stream().map(ChunkForEmbedding::text).toList();
      List<float[]> vectors = embeddingProvider.get().embed(inputs);
      if (vectors.size() != batch.size()) {
        throw new IllegalStateException("Embedding provider returned an unexpected result count");
      }

      for (int i = 0; i < batch.size(); i++) {
        float[] vector = vectors.get(i);
        if (vector == null || vector.length != EMBEDDING_DIMENSIONS) {
          throw new IllegalStateException(
              "Embedding provider must return vectors with " + EMBEDDING_DIMENSIONS + " dimensions");
        }
        embeddingRepository.upsert(batch.get(i).chunkId(), vector, embeddingProvider.get().model());
        embedded++;
      }
    }
    return embedded;
  }

  public BackfillResult backfillAllDocuments() {
    if (embeddingProvider.isEmpty()) {
      return new BackfillResult(false, 0, 0);
    }

    List<Long> documentIds = embeddingRepository.findDocumentIdsWithMissingEmbeddings();
    int embedded = 0;
    for (long documentId : documentIds) {
      embedded += backfillDocument(documentId);
    }
    return new BackfillResult(true, documentIds.size(), embedded);
  }

  public record BackfillResult(boolean configured, int documents, int embeddings) {}
}
