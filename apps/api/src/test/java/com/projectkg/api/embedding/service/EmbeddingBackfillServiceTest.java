package com.projectkg.api.embedding.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

  @Test
  void shouldRequireReindexWhenPersistedEmbeddingIdentityDiffers() {
    FakeEmbeddingRepository repository = new FakeEmbeddingRepository(List.of());
    repository.identities = List.of(new EmbeddingIdentity("openai", "text-embedding-3-small", 1536));
    EmbeddingBackfillService service = new EmbeddingBackfillService(
        repository, Optional.of(new FixedEmbeddingProvider()));

    assertThrows(EmbeddingReindexRequiredException.class, () -> service.backfillAllDocuments());
  }

  @Test
  void shouldDeleteExistingEmbeddingsBeforeExplicitReindex() {
    FakeEmbeddingRepository repository = new FakeEmbeddingRepository(List.of(new ChunkForEmbedding(11L, "first")));
    repository.documentIds = List.of(7L);
    EmbeddingBackfillService service = new EmbeddingBackfillService(
        repository, Optional.of(new FixedEmbeddingProvider()));

    service.reindexAll();

    assertEquals(1, repository.deleteCalls);
    assertEquals(List.of(11L), repository.upsertedChunkIds);
  }

  @Test
  void shouldRejectVectorsWithUnexpectedDimensions() {
    FakeEmbeddingRepository repository = new FakeEmbeddingRepository(List.of(new ChunkForEmbedding(11L, "first")));
    EmbeddingBackfillService service = new EmbeddingBackfillService(
        repository, Optional.of(new WrongDimensionEmbeddingProvider()));

    assertThrows(IllegalStateException.class, () -> service.backfillDocument(7L));
    assertEquals(List.of(), repository.upsertedChunkIds);
  }

  private static final class FakeEmbeddingRepository implements EmbeddingRepository {
    private final List<ChunkForEmbedding> chunks;
    private final List<Long> upsertedChunkIds = new ArrayList<>();
    private List<Long> documentIds = List.of();
    private List<EmbeddingIdentity> identities = List.of();
    private int findCalls;
    private int deleteCalls;
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
    public void upsert(long chunkId, float[] vector, EmbeddingIdentity identity) {
      upsertedChunkIds.add(chunkId);
      this.model = identity.model();
    }

    @Override
    public List<EmbeddingIdentity> findEmbeddingIdentities() {
      return identities;
    }

    @Override
    public void deleteAll() {
      deleteCalls++;
      identities = List.of();
    }
  }

  private static class FixedEmbeddingProvider implements EmbeddingProvider {
    @Override
    public String provider() {
      return "ollama";
    }

    @Override
    public String model() {
      return "test-embedding";
    }

    @Override
    public int dimensions() {
      return 1024;
    }

    @Override
    public List<float[]> embed(List<String> inputs) {
      return inputs.stream().map(input -> new float[1024]).toList();
    }
  }

  private static final class WrongDimensionEmbeddingProvider extends FixedEmbeddingProvider {
    @Override
    public List<float[]> embed(List<String> inputs) {
      return inputs.stream().map(input -> new float[1536]).toList();
    }
  }
}
