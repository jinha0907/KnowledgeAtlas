package com.projectkg.api.embedding.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.projectkg.api.embedding.repository.EmbeddingRepository;
import com.projectkg.api.embedding.repository.EmbeddingRepository.ChunkForEmbedding;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class EmbeddingBackfillServiceTest {

  @Test
  void shouldEmbedOnlyChunksWithoutAnEmbedding() {
    FakeEmbeddingRepository repository = new FakeEmbeddingRepository(List.of(
        new ChunkForEmbedding(11L, "first"),
        new ChunkForEmbedding(12L, "second")));
    EmbeddingBackfillService service = new EmbeddingBackfillService(
        repository,
        Optional.of(new FixedEmbeddingProvider()));

    int embedded = service.backfillDocument(7L);

    assertEquals(2, embedded);
    assertEquals(List.of(11L, 12L), repository.upsertedChunkIds);
    assertEquals("test-embedding", repository.model);
  }

  @Test
  void shouldNotQueryForChunksWhenNoProviderIsConfigured() {
    FakeEmbeddingRepository repository = new FakeEmbeddingRepository(List.of(new ChunkForEmbedding(11L, "first")));
    EmbeddingBackfillService service = new EmbeddingBackfillService(repository, Optional.empty());

    assertEquals(0, service.backfillDocument(7L));
    assertEquals(0, repository.findCalls);
  }

  @Test
  void shouldBackfillEveryDocumentWithMissingEmbeddings() {
    FakeEmbeddingRepository repository = new FakeEmbeddingRepository(List.of(new ChunkForEmbedding(11L, "first")));
    repository.documentIds = List.of(7L, 8L);
    EmbeddingBackfillService service = new EmbeddingBackfillService(
        repository,
        Optional.of(new FixedEmbeddingProvider()));

    EmbeddingBackfillService.BackfillResult result = service.backfillAllDocuments();

    assertEquals(true, result.configured());
    assertEquals(2, result.documents());
    assertEquals(2, result.embeddings());
  }

  private static final class FakeEmbeddingRepository implements EmbeddingRepository {
    private final List<ChunkForEmbedding> chunks;
    private final List<Long> upsertedChunkIds = new ArrayList<>();
    private List<Long> documentIds = List.of();
    private int findCalls;
    private String model;

    private FakeEmbeddingRepository(List<ChunkForEmbedding> chunks) {
      this.chunks = chunks;
    }

    @Override
    public List<Long> findDocumentIdsWithMissingEmbeddings() {
      return documentIds;
    }

    @Override
    public List<ChunkForEmbedding> findChunksWithoutEmbedding(long documentId) {
      findCalls++;
      return chunks;
    }

    @Override
    public void upsert(long chunkId, float[] vector, String model) {
      upsertedChunkIds.add(chunkId);
      this.model = model;
    }
  }

  private static final class FixedEmbeddingProvider implements EmbeddingProvider {
    @Override
    public String model() {
      return "test-embedding";
    }

    @Override
    public List<float[]> embed(List<String> inputs) {
      return inputs.stream().map(input -> new float[1536]).toList();
    }
  }
}
