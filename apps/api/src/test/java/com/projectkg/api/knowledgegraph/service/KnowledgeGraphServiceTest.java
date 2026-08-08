package com.projectkg.api.knowledgegraph.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.projectkg.api.embedding.service.EmbeddingIdentity;
import com.projectkg.api.embedding.service.EmbeddingProvider;
import com.projectkg.api.knowledgegraph.dto.KnowledgeGraphDto;
import com.projectkg.api.knowledgegraph.dto.KnowledgeGraphRebuildResponse;
import com.projectkg.api.knowledgegraph.repository.KnowledgeGraphRepository;
import com.projectkg.api.knowledgegraph.repository.KnowledgeGraphRepository.DocumentNodeRow;
import com.projectkg.api.knowledgegraph.repository.KnowledgeGraphRepository.SimilarityCandidate;
import com.projectkg.api.knowledgegraph.repository.KnowledgeGraphRepository.SimilarityEdge;
import com.projectkg.api.knowledgegraph.repository.KnowledgeGraphRepository.SimilarityEdgeRow;
import com.projectkg.api.knowledgegraph.repository.KnowledgeGraphRepository.SimilarityEvidence;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class KnowledgeGraphServiceTest {

  @Test
  void shouldBuildStableDocumentNodesAndCitedSimilarityEdges() {
    KnowledgeGraphDto graph = KnowledgeGraphService.assemble(
        "ready",
        new EmbeddingIdentity("ollama", "bge-m3", 1024),
        List.of(
            new DocumentNodeRow(4L, "Design notes", "Overview", List.of("design"), 12),
            new DocumentNodeRow(2L, "", null, List.of(), 3)),
        List.of(new SimilarityEdgeRow(
            2L, 4L, 0.82, 9L, "block-2", "Related excerpt", 10L, "block-4", "Matching excerpt")));

    assertEquals(List.of("document-2", "document-4"),
        graph.nodes().stream().map(node -> node.id()).toList());
    assertEquals("Untitled document", graph.nodes().getFirst().label());
    assertEquals("similar-2-4", graph.edges().getFirst().id());
    assertEquals("block-2", graph.edges().getFirst().sourceCitation().blockId());
    assertEquals("block-4", graph.edges().getFirst().targetCitation().blockId());
    assertEquals("ollama", graph.embeddingIdentity().provider());
  }

  @Test
  void shouldNotDeleteExistingGraphWhenEmbeddingProviderIsDisabled() {
    RecordingRepository repository = new RecordingRepository();
    KnowledgeGraphService service = new KnowledgeGraphService(repository, Optional.empty(), 0.35);

    KnowledgeGraphRebuildResponse result = service.rebuild();

    assertEquals("disabled", result.status());
    assertEquals(0, result.documents());
    assertEquals(0, result.edges());
    assertEquals(0, repository.deleteCalls);
  }

  @Test
  void shouldPersistOnlyCandidatesWithRepresentativeEvidence() {
    RecordingRepository repository = new RecordingRepository();
    repository.candidates = List.of(
        new SimilarityCandidate(1L, 2L, 0.71, "checksum-1", "checksum-2"),
        new SimilarityCandidate(1L, 3L, 0.51, "checksum-1", "checksum-3"));
    repository.evidence = Optional.of(new SimilarityEvidence(10L, 20L));
    KnowledgeGraphService service = new KnowledgeGraphService(
        repository, Optional.of(new TestEmbeddingProvider()), 0.35);

    KnowledgeGraphRebuildResponse result = service.rebuild();

    assertEquals("success", result.status());
    assertEquals(3, result.documents());
    assertEquals(2, result.edges());
    assertEquals(1, repository.deleteCalls);
    assertEquals(2, repository.upsertedEdges.size());
    assertEquals(10L, repository.upsertedEdges.getFirst().chunkIdLow());
    assertEquals(20L, repository.upsertedEdges.getFirst().chunkIdHigh());
  }

  private static final class TestEmbeddingProvider implements EmbeddingProvider {
    @Override
    public String provider() {
      return "ollama";
    }

    @Override
    public String model() {
      return "bge-m3";
    }

    @Override
    public int dimensions() {
      return 1024;
    }

    @Override
    public List<float[]> embed(List<String> inputs) {
      return List.of();
    }
  }

  private static final class RecordingRepository implements KnowledgeGraphRepository {
    private int deleteCalls;
    private List<SimilarityCandidate> candidates = List.of();
    private Optional<SimilarityEvidence> evidence = Optional.empty();
    private final List<SimilarityEdge> upsertedEdges = new java.util.ArrayList<>();

    @Override
    public void deleteAll() {
      deleteCalls++;
    }

    @Override
    public int countDocumentsWithEmbeddings(EmbeddingIdentity identity) {
      return 3;
    }

    @Override
    public List<SimilarityCandidate> findSparseCandidates(
        EmbeddingIdentity identity, double minimumScore, int maxNeighbors
    ) {
      return candidates;
    }

    @Override
    public Optional<SimilarityEvidence> findBestEvidence(
        long documentIdLow, long documentIdHigh, EmbeddingIdentity identity, int representativeChunkLimit
    ) {
      return evidence;
    }

    @Override
    public void upsert(SimilarityEdge edge) {
      upsertedEdges.add(edge);
    }

    @Override
    public List<DocumentNodeRow> findDocumentNodes() {
      return List.of();
    }

    @Override
    public List<SimilarityEdgeRow> findEdges(EmbeddingIdentity identity, double minimumScore) {
      return List.of();
    }

    @Override
    public boolean hasEdges() {
      return false;
    }
  }
}
