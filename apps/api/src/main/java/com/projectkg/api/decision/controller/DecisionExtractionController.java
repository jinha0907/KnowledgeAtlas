package com.projectkg.api.decision.controller;

import com.projectkg.api.decision.dto.DecisionExtractionResponse;
import com.projectkg.api.decision.service.DecisionExtractionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documents")
@Tag(name = "decision", description = "Decision extraction and lifecycle APIs")
public class DecisionExtractionController {
  private final DecisionExtractionService decisionExtractionService;

  public DecisionExtractionController(DecisionExtractionService decisionExtractionService) {
    this.decisionExtractionService = decisionExtractionService;
  }

  @PostMapping("/{documentId}/decisions/extract")
  @Operation(summary = "Extract proposed decisions from one synced document")
  public ResponseEntity<DecisionExtractionResponse> extract(@PathVariable long documentId) {
    return ResponseEntity.ok(decisionExtractionService.extract(documentId));
  }
}
