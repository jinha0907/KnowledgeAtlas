package com.projectkg.api.search.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.projectkg.api.search.dto.SearchRequest;
import com.projectkg.api.search.dto.SearchResponse;
import com.projectkg.api.search.repository.SearchRepository;
import com.projectkg.api.search.repository.SearchRepository.SearchRow;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

  @Mock
  private SearchRepository searchRepository;

  @InjectMocks
  private SearchService searchService;

  @Test
  void shouldReturnNoEvidenceMessageWhenNoRowsFound() {
    when(searchRepository.searchByKeyword("missing", 5)).thenReturn(List.of());

    SearchResponse response = searchService.search(new SearchRequest("missing", null));

    assertEquals("No evidence found for the query.", response.answer());
    assertTrue(response.citations().isEmpty());
  }

  @Test
  void shouldReturnEvidenceWhenRowsFound() {
    when(searchRepository.searchByKeyword("query", 3)).thenReturn(List.of(
        new SearchRow(1L, 10L, "b1", "Doc", "Text", 0.91)
    ));

    SearchResponse response = searchService.search(new SearchRequest("query", 3));

    assertEquals("Evidence found. See citations.", response.answer());
    assertEquals(1, response.citations().size());
    assertEquals(10L, response.citations().getFirst().documentId());
  }
}
