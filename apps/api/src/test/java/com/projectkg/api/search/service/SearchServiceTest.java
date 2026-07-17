package com.projectkg.api.search.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.projectkg.api.search.dto.SearchRequest;
import com.projectkg.api.search.dto.SearchResponse;
import com.projectkg.api.search.repository.SearchRepository;
import com.projectkg.api.search.repository.SearchRepository.SearchRow;
import java.util.List;
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
}
