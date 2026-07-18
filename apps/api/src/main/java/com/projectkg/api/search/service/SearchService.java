package com.projectkg.api.search.service;

import com.projectkg.api.embedding.service.EmbeddingIdentity;
import com.projectkg.api.embedding.service.EmbeddingProvider;
import com.projectkg.api.embedding.service.EmbeddingReindexRequiredException;
import com.projectkg.api.search.dto.SearchCitationDto;
import com.projectkg.api.search.dto.SearchRequest;
import com.projectkg.api.search.dto.SearchResponse;
import com.projectkg.api.search.repository.SearchRepository;
import com.projectkg.api.search.repository.SearchRepository.SearchRow;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SearchService {
  private static final Logger logger = LoggerFactory.getLogger(SearchService.class);
  private static final int DEFAULT_TOP_K = 5;
  private static final int MAX_TOP_K = 20;

  private final SearchRepository searchRepository;
  private final Optional<EmbeddingProvider> embeddingProvider;

  public SearchService(SearchRepository searchRepository) {
    this(searchRepository, Optional.empty());
  }

  @Autowired
  public SearchService(SearchRepository searchRepository, Optional<EmbeddingProvider> embeddingProvider) {
    this.searchRepository = searchRepository;
    this.embeddingProvider = embeddingProvider;
  }

  public SearchResponse search(SearchRequest request) {
    if (request == null || request.query() == null || request.query().isBlank()) {
      throw new IllegalArgumentException("query is required");
    }

    int topK = sanitizeTopK(request.topK());
    String query = request.query().trim();
    List<SearchRow> rows = search(query, topK);

    List<SearchCitationDto> citations = rows.stream()
        .map(row -> new SearchCitationDto(
            row.score(),
            row.documentId(),
            row.blockId(),
            row.title(),
            row.text()))
        .toList();

    String answer = citations.isEmpty()
        ? "No evidence found for the query."
        : "Evidence found. See citations.";

    return new SearchResponse(answer, citations);
  }

  private List<SearchRow> search(String query, int topK) {
    if (embeddingProvider.isEmpty()) {
      return searchRepository.searchByKeyword(query, topK);
    }

    try {
      List<float[]> embeddings = embeddingProvider.get().embed(List.of(query));
      if (embeddings.size() != 1 || embeddings.getFirst().length != embeddingProvider.get().dimensions()) {
        throw new IllegalStateException("Embedding provider returned an invalid query vector");
      }
      EmbeddingIdentity identity = new EmbeddingIdentity(
          embeddingProvider.get().provider(),
          embeddingProvider.get().model(),
          embeddingProvider.get().dimensions());
      List<EmbeddingIdentity> persisted = searchRepository.findEmbeddingIdentities();
      if (!persisted.isEmpty() && !persisted.equals(List.of(identity))) {
        throw new EmbeddingReindexRequiredException(identity, persisted.toString());
      }
      return searchRepository.searchByHybrid(query, embeddings.getFirst(), identity, topK);
    } catch (EmbeddingReindexRequiredException ex) {
      logger.warn("Hybrid retrieval requires a confirmed re-index; falling back to keyword search");
      return searchRepository.searchByKeyword(query, topK);
    } catch (RuntimeException ex) {
      logger.warn("Hybrid retrieval unavailable; falling back to keyword search", ex);
      return searchRepository.searchByKeyword(query, topK);
    }
  }

  private int sanitizeTopK(Integer topK) {
    if (topK == null || topK <= 0) {
      return DEFAULT_TOP_K;
    }
    return Math.min(topK, MAX_TOP_K);
  }
}
