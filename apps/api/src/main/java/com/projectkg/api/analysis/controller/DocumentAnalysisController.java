package com.projectkg.api.analysis.controller;

import com.projectkg.api.analysis.dto.DocumentAnalysisResponse;
import com.projectkg.api.analysis.service.DocumentAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documents")
@Tag(name = "document", description = "Synced source document read and analysis APIs")
public class DocumentAnalysisController {
  private final DocumentAnalysisService documentAnalysisService;

  public DocumentAnalysisController(DocumentAnalysisService documentAnalysisService) {
    this.documentAnalysisService = documentAnalysisService;
  }

  @PostMapping("/{documentId}/analysis/run")
  @Operation(summary = "Create a reviewable summary and tags for one synced document")
  public ResponseEntity<DocumentAnalysisResponse> analyze(@PathVariable long documentId) {
    return ResponseEntity.ok(documentAnalysisService.analyze(documentId));
  }
}
