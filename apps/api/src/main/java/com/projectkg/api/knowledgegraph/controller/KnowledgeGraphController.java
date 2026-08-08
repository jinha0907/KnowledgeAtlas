package com.projectkg.api.knowledgegraph.controller;

import com.projectkg.api.knowledgegraph.dto.KnowledgeGraphDto;
import com.projectkg.api.knowledgegraph.dto.KnowledgeGraphRebuildResponse;
import com.projectkg.api.knowledgegraph.service.KnowledgeGraphService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/knowledge-graph")
@Tag(name = "knowledge-graph", description = "Embedding-backed related-document graph APIs")
public class KnowledgeGraphController {
  private final KnowledgeGraphService knowledgeGraphService;

  public KnowledgeGraphController(KnowledgeGraphService knowledgeGraphService) {
    this.knowledgeGraphService = knowledgeGraphService;
  }

  @GetMapping
  @Operation(summary = "Get the persisted, explainable semantic knowledge graph")
  public ResponseEntity<KnowledgeGraphDto> getGraph(
      @RequestParam(required = false) Double minimumScore
  ) {
    return ResponseEntity.ok(knowledgeGraphService.getGraph(minimumScore));
  }

  @PostMapping("/rebuild")
  @Operation(summary = "Rebuild sparse document similarity edges from current embeddings")
  public ResponseEntity<KnowledgeGraphRebuildResponse> rebuild() {
    return ResponseEntity.ok(knowledgeGraphService.rebuild());
  }
}
