package com.projectkg.api.search.controller;

import com.projectkg.api.search.dto.SearchRequest;
import com.projectkg.api.search.dto.SearchResponse;
import com.projectkg.api.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "retrieval", description = "Search and retrieval APIs")
public class SearchController {
  private final SearchService searchService;

  public SearchController(SearchService searchService) {
    this.searchService = searchService;
  }

  @PostMapping("/search")
  @Operation(summary = "Search chunks and return evidence citations")
  public ResponseEntity<SearchResponse> search(@RequestBody SearchRequest request) {
    return ResponseEntity.ok(searchService.search(request));
  }
}
