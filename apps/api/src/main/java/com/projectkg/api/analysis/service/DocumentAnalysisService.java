package com.projectkg.api.analysis.service;

import com.projectkg.api.analysis.dto.DocumentAnalysisDto;
import com.projectkg.api.analysis.dto.DocumentAnalysisResponse;
import com.projectkg.api.analysis.provider.DocumentAnalysisProvider;
import com.projectkg.api.analysis.provider.DocumentAnalysisProvider.AnalysisResult;
import com.projectkg.api.analysis.repository.DocumentAnalysisRunRepository;
import com.projectkg.api.analysis.repository.DocumentAnalysisRunRepository.AnalysisRunRow;
import com.projectkg.api.decision.extraction.DecisionExtractionProvider.SourceBlock;
import com.projectkg.api.notion.repository.ContentBlockRepository;
import com.projectkg.api.notion.repository.ContentBlockRepository.ContentBlockRow;
import com.projectkg.api.notion.repository.SourceDocumentRepository;
import com.projectkg.api.notion.repository.SourceDocumentRepository.SourceDocumentDetailRow;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class DocumentAnalysisService {
  static final int MAX_SUMMARY_LENGTH = 800;
  static final int MAX_TAGS = 8;
  static final int MAX_TAG_LENGTH = 80;

  private final SourceDocumentRepository sourceDocumentRepository;
  private final ContentBlockRepository contentBlockRepository;
  private final DocumentAnalysisRunRepository analysisRunRepository;
  private final Optional<DocumentAnalysisProvider> analysisProvider;

  public DocumentAnalysisService(
      SourceDocumentRepository sourceDocumentRepository,
      ContentBlockRepository contentBlockRepository,
      DocumentAnalysisRunRepository analysisRunRepository,
      Optional<DocumentAnalysisProvider> analysisProvider
  ) {
    this.sourceDocumentRepository = sourceDocumentRepository;
    this.contentBlockRepository = contentBlockRepository;
    this.analysisRunRepository = analysisRunRepository;
    this.analysisProvider = analysisProvider;
  }

  public DocumentAnalysisResponse analyze(long documentId) {
    if (analysisProvider.isEmpty()) {
      return new DocumentAnalysisResponse("disabled", null);
    }

    SourceDocumentDetailRow document = sourceDocumentRepository.findById(documentId)
        .orElseThrow(() -> new IllegalArgumentException("source document not found: " + documentId));
    Optional<AnalysisRunRow> existing = analysisRunRepository
        .findByDocumentAndChecksum(documentId, document.checksum());
    if (existing.isPresent() && "success".equals(existing.get().status())) {
      return new DocumentAnalysisResponse("existing", toDto(existing.get()));
    }
    if (existing.isPresent() && "running".equals(existing.get().status())) {
      throw new DocumentAnalysisAlreadyRunningException(documentId);
    }

    long runId;
    if (existing.isPresent()) {
      analysisRunRepository.retryFailed(existing.get().id());
      runId = existing.get().id();
    } else {
      try {
        runId = analysisRunRepository.createRunning(documentId, document.checksum());
      } catch (DocumentAnalysisRunRepository.AnalysisAlreadyRunningException ex) {
        throw new DocumentAnalysisAlreadyRunningException(documentId);
      }
    }

    try {
      List<ContentBlockRow> blocks = contentBlockRepository.findByDocumentId(documentId);
      AnalysisResult result = validateResult(analysisProvider.get().analyze(
          document.title(),
          blocks.stream().map(block -> new SourceBlock(block.blockId(), block.text())).toList()));
      analysisRunRepository.markSuccess(runId, result.summary(), result.tags());
      DocumentAnalysisDto analysis = analysisRunRepository.findByDocumentAndChecksum(
          documentId, document.checksum()).map(this::toDto)
          .orElseThrow(() -> new IllegalStateException("Document analysis result was not persisted"));
      return new DocumentAnalysisResponse("success", analysis);
    } catch (RuntimeException ex) {
      analysisRunRepository.markFailed(runId, ex.getMessage());
      throw ex;
    }
  }

  public Optional<DocumentAnalysisDto> currentForDocument(long documentId, String sourceChecksum) {
    return analysisRunRepository.findByDocumentAndChecksum(documentId, sourceChecksum).map(this::toDto);
  }

  static AnalysisResult validateResult(AnalysisResult result) {
    if (result == null || result.summary() == null) {
      throw new IllegalArgumentException("Document analysis must include a summary and tags");
    }
    String summary = result.summary().trim();
    if (summary.isBlank() || summary.length() > MAX_SUMMARY_LENGTH) {
      throw new IllegalArgumentException("Document analysis summary must be 1 to 800 characters");
    }

    Map<String, String> tags = new LinkedHashMap<>();
    for (String tag : result.tags() == null ? List.<String>of() : result.tags()) {
      if (tag == null) {
        continue;
      }
      String normalized = tag.trim().replaceAll("\\s+", " ");
      if (normalized.isBlank() || normalized.length() > MAX_TAG_LENGTH) {
        throw new IllegalArgumentException("Document analysis tags must be 1 to 80 characters");
      }
      tags.putIfAbsent(normalized.toLowerCase(Locale.ROOT), normalized);
    }
    if (tags.isEmpty() || tags.size() > MAX_TAGS) {
      throw new IllegalArgumentException("Document analysis must contain 1 to 8 tags");
    }
    return new AnalysisResult(summary, new ArrayList<>(tags.values()));
  }

  private DocumentAnalysisDto toDto(AnalysisRunRow row) {
    return new DocumentAnalysisDto(
        row.id(), row.status(), row.summary(), row.tags(), row.completedAt());
  }

  public static class DocumentAnalysisAlreadyRunningException extends RuntimeException {
    public DocumentAnalysisAlreadyRunningException(long documentId) {
      super("Document analysis is already running for document: " + documentId);
    }
  }
}
