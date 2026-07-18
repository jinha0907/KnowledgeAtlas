package com.projectkg.api.document.service;

import com.projectkg.api.analysis.dto.DocumentAnalysisDto;
import com.projectkg.api.analysis.service.DocumentAnalysisService;
import com.projectkg.api.document.dto.DocumentBlockDto;
import com.projectkg.api.document.dto.DocumentDetailDto;
import com.projectkg.api.document.dto.DocumentSummaryDto;
import com.projectkg.api.notion.repository.ContentBlockRepository;
import com.projectkg.api.notion.repository.SourceDocumentRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DocumentService {
  private final SourceDocumentRepository sourceDocumentRepository;
  private final ContentBlockRepository contentBlockRepository;
  private final DocumentAnalysisService documentAnalysisService;

  public DocumentService(
      SourceDocumentRepository sourceDocumentRepository,
      ContentBlockRepository contentBlockRepository,
      DocumentAnalysisService documentAnalysisService
  ) {
    this.sourceDocumentRepository = sourceDocumentRepository;
    this.contentBlockRepository = contentBlockRepository;
    this.documentAnalysisService = documentAnalysisService;
  }

  public List<DocumentSummaryDto> list() {
    return sourceDocumentRepository.findAll().stream().map(this::toSummary).toList();
  }

  public DocumentDetailDto getById(long documentId) {
    SourceDocumentRepository.SourceDocumentDetailRow document = sourceDocumentRepository.findById(documentId)
        .orElseThrow(() -> new IllegalArgumentException("source document not found: " + documentId));
    List<DocumentBlockDto> blocks = contentBlockRepository.findByDocumentId(documentId).stream()
        .map(block -> new DocumentBlockDto(block.blockId(), block.text(), block.path()))
        .toList();
    return new DocumentDetailDto(
        document.id(), document.title(), document.lastSyncedAt(), blocks,
        documentAnalysisService.currentForDocument(documentId, document.checksum()).orElse(null));
  }

  private DocumentSummaryDto toSummary(SourceDocumentRepository.SourceDocumentSummaryRow row) {
    return new DocumentSummaryDto(
        row.id(), row.sourceType(), row.sourceId(), row.title(), row.lastSyncedAt(),
        documentAnalysisService.currentForDocument(row.id(), row.checksum()).orElse(null));
  }
}
