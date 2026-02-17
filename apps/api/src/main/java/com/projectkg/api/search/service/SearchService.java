package com.projectkg.api.search.service;

import com.projectkg.api.search.dto.SearchCitationDto;
import com.projectkg.api.search.dto.SearchRequest;
import com.projectkg.api.search.dto.SearchResponse;
import com.projectkg.api.search.repository.SearchRepository;
import com.projectkg.api.search.repository.SearchRepository.SearchRow;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SearchService {
  private static final int DEFAULT_TOP_K = 5;
  private static final int MAX_TOP_K = 20;

  private final SearchRepository searchRepository;

  public SearchService(SearchRepository searchRepository) {
    this.searchRepository = searchRepository;
  }

  public SearchResponse search(SearchRequest request) {
    if (request == null || request.query() == null || request.query().isBlank()) {
      throw new IllegalArgumentException("query is required");
    }

    int topK = sanitizeTopK(request.topK());
    List<SearchRow> rows = searchRepository.searchByKeyword(request.query().trim(), topK);

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

  private int sanitizeTopK(Integer topK) {
    if (topK == null || topK <= 0) {
      return DEFAULT_TOP_K;
    }
    return Math.min(topK, MAX_TOP_K);
  }
}
