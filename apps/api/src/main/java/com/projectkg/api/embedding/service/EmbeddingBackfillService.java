package com.projectkg.api.embedding.service;

import com.projectkg.api.embedding.repository.EmbeddingRepository;
import com.projectkg.api.embedding.repository.EmbeddingRepository.ChunkForEmbedding;
import com.projectkg.api.embedding.dto.EmbeddingIdentityDto;
import com.projectkg.api.embedding.dto.EmbeddingStatusResponse;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class EmbeddingBackfillService {
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
    assertCompatibleConfiguration();

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
        if (vector == null || vector.length != embeddingProvider.get().dimensions()) {
          throw new IllegalStateException(
              "Embedding provider must return vectors with " + embeddingProvider.get().dimensions() + " dimensions");
        }
        embeddingRepository.upsert(batch.get(i).chunkId(), vector, activeIdentity());
        embedded++;
      }
    }
    return embedded;
  }

  public BackfillResult backfillAllDocuments() {
    if (embeddingProvider.isEmpty()) {
      return new BackfillResult(false, 0, 0);
    }
    assertCompatibleConfiguration();

    List<Long> documentIds = embeddingRepository.findDocumentIdsWithMissingEmbeddings();
    int embedded = 0;
    for (long documentId : documentIds) {
      embedded += backfillDocument(documentId);
    }
    return new BackfillResult(true, documentIds.size(), embedded);
  }

  public BackfillResult reindexAll() {
    if (embeddingProvider.isEmpty()) {
      return new BackfillResult(false, 0, 0);
    }
    embeddingRepository.deleteAll();
    return backfillAllDocuments();
  }

  public void assertCompatibleConfiguration() {
    if (embeddingProvider.isEmpty()) {
      return;
    }
    List<EmbeddingIdentity> identities = embeddingRepository.findEmbeddingIdentities();
    EmbeddingIdentity active = activeIdentity();
    if (!identities.isEmpty() && (identities.size() != 1 || !active.equals(identities.getFirst()))) {
      throw new EmbeddingReindexRequiredException(active, identities.toString());
    }
  }

  public EmbeddingIdentity activeIdentity() {
    if (embeddingProvider.isEmpty()) {
      throw new IllegalStateException("No embedding provider is configured");
    }
    EmbeddingProvider provider = embeddingProvider.get();
    return new EmbeddingIdentity(provider.provider(), provider.model(), provider.dimensions());
  }

  public EmbeddingStatusResponse status() {
    List<EmbeddingIdentity> persisted = embeddingRepository.findEmbeddingIdentities();
    long eligibleChunks = embeddingRepository.countEligibleChunks();
    long embeddedChunks = embeddingRepository.countEmbeddings();
    if (embeddingProvider.isEmpty()) {
      return new EmbeddingStatusResponse(
          "disabled", null, persisted.stream().map(EmbeddingIdentityDto::from).toList(),
          eligibleChunks, embeddedChunks, Math.max(0, eligibleChunks - embeddedChunks), false);
    }

    EmbeddingIdentity active = activeIdentity();
    boolean reindexRequired = !persisted.isEmpty() && !persisted.equals(List.of(active));
    String status = reindexRequired ? "reindex_required"
        : eligibleChunks == embeddedChunks ? "ready" : "incomplete";
    return new EmbeddingStatusResponse(
        status, EmbeddingIdentityDto.from(active), persisted.stream().map(EmbeddingIdentityDto::from).toList(),
        eligibleChunks, embeddedChunks, Math.max(0, eligibleChunks - embeddedChunks), reindexRequired);
  }

  public record BackfillResult(boolean configured, int documents, int embeddings) {}
}
