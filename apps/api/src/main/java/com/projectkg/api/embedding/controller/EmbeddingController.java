package com.projectkg.api.embedding.controller;

import com.projectkg.api.embedding.dto.EmbeddingBackfillResponse;
import com.projectkg.api.embedding.service.EmbeddingBackfillService;
import com.projectkg.api.embedding.service.EmbeddingBackfillService.BackfillResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/embeddings")
@Tag(name = "embedding", description = "Embedding generation and backfill APIs")
public class EmbeddingController {
  private final EmbeddingBackfillService embeddingBackfillService;

  public EmbeddingController(EmbeddingBackfillService embeddingBackfillService) {
    this.embeddingBackfillService = embeddingBackfillService;
  }

  @PostMapping("/backfill")
  @Operation(summary = "Generate embeddings for every chunk that is still missing one")
  public ResponseEntity<EmbeddingBackfillResponse> backfill() {
    BackfillResult result = embeddingBackfillService.backfillAllDocuments();
    String status = result.configured() ? "success" : "disabled";
    return ResponseEntity.ok(new EmbeddingBackfillResponse(status, result.documents(), result.embeddings()));
  }
}
