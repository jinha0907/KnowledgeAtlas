package com.projectkg.api.document.controller;

import com.projectkg.api.document.dto.DocumentDetailDto;
import com.projectkg.api.document.dto.DocumentSummaryDto;
import com.projectkg.api.document.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documents")
@Tag(name = "document", description = "Synced source document read APIs")
public class DocumentController {
  private final DocumentService documentService;

  public DocumentController(DocumentService documentService) {
    this.documentService = documentService;
  }

  @GetMapping
  @Operation(summary = "List synced documents")
  public ResponseEntity<List<DocumentSummaryDto>> list() {
    return ResponseEntity.ok(documentService.list());
  }

  @GetMapping("/{documentId}")
  @Operation(summary = "Get a synced document and its blocks")
  public ResponseEntity<DocumentDetailDto> getById(@PathVariable long documentId) {
    return ResponseEntity.ok(documentService.getById(documentId));
  }
}
