package com.projectkg.api.embedding.controller;

import com.projectkg.api.embedding.dto.EmbeddingBackfillResponse;
import com.projectkg.api.embedding.dto.EmbeddingReindexRequest;
import com.projectkg.api.embedding.dto.EmbeddingReindexResponse;
import com.projectkg.api.embedding.dto.EmbeddingStatusResponse;
import com.projectkg.api.embedding.service.EmbeddingBackfillService;
import com.projectkg.api.embedding.service.EmbeddingBackfillService.BackfillResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

  @GetMapping("/status")
  @Operation(summary = "Get configured and persisted embedding readiness without calling a provider")
  public ResponseEntity<EmbeddingStatusResponse> status() {
    return ResponseEntity.ok(embeddingBackfillService.status());
  }

  @PostMapping("/reindex")
  @Operation(summary = "Delete every embedding and rebuild with the configured provider; requires confirm=true")
  public ResponseEntity<EmbeddingReindexResponse> reindex(@RequestBody(required = false) EmbeddingReindexRequest request) {
    if (request == null || !Boolean.TRUE.equals(request.confirm())) {
      throw new IllegalArgumentException("Re-indexing deletes all embeddings; send {\"confirm\":true}");
    }
    BackfillResult result = embeddingBackfillService.reindexAll();
    String status = result.configured() ? "success" : "disabled";
    return ResponseEntity.ok(new EmbeddingReindexResponse(status, result.documents(), result.embeddings()));
  }
}
