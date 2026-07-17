package com.projectkg.api.search.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.projectkg.api.embedding.service.EmbeddingProvider;
import com.projectkg.api.search.dto.SearchRequest;
import com.projectkg.api.search.dto.SearchResponse;
import com.projectkg.api.search.repository.SearchRepository;
import com.projectkg.api.search.repository.SearchRepository.SearchRow;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SearchServiceTest {

  @Test
  void shouldReturnNoEvidenceMessageWhenNoRowsFound() {
    SearchService searchService = new SearchService((query, topK) -> List.of());

    SearchResponse response = searchService.search(new SearchRequest("missing", null));

    assertEquals("No evidence found for the query.", response.answer());
    assertTrue(response.citations().isEmpty());
  }

  @Test
  void shouldReturnEvidenceWhenRowsFound() {
    SearchRepository repository = (query, topK) -> List.of(
        new SearchRow(1L, 10L, "b1", "Doc", "Text", 0.91)
    );
    SearchService searchService = new SearchService(repository);

    SearchResponse response = searchService.search(new SearchRequest("query", 3));

    assertEquals("Evidence found. See citations.", response.answer());
    assertEquals(1, response.citations().size());
    assertEquals(10L, response.citations().getFirst().documentId());
  }

  @Test
  void shouldUseHybridRetrievalWhenAnEmbeddingProviderIsConfigured() {
    HybridSearchRepository repository = new HybridSearchRepository();
    SearchService searchService = new SearchService(repository, Optional.of(new FixedEmbeddingProvider()));

    SearchResponse response = searchService.search(new SearchRequest("semantic query", 3));

    assertTrue(repository.hybridCalled);
    assertEquals("Evidence found. See citations.", response.answer());
  }

  private static final class HybridSearchRepository implements SearchRepository {
    private boolean hybridCalled;

    @Override
    public List<SearchRow> searchByKeyword(String query, int topK) {
      return List.of();
    }

    @Override
    public List<SearchRow> searchByHybrid(String query, float[] queryEmbedding, int topK) {
      hybridCalled = true;
      return List.of(new SearchRow(2L, 20L, "b2", "Semantic doc", "Semantic text", 0.03));
    }
  }

  private static final class FixedEmbeddingProvider implements EmbeddingProvider {
    @Override
    public String model() {
      return "test";
    }

    @Override
    public List<float[]> embed(List<String> inputs) {
      return List.of(new float[1536]);
    }
  }
}
