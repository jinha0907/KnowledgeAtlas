package com.projectkg.api.decision.service;

import com.projectkg.api.decision.dto.DecisionDto;
import com.projectkg.api.decision.dto.DecisionExtractionResponse;
import com.projectkg.api.decision.extraction.DecisionExtractionProvider;
import com.projectkg.api.decision.extraction.DecisionExtractionProvider.DecisionCandidate;
import com.projectkg.api.decision.extraction.DecisionExtractionProvider.EvidenceCandidate;
import com.projectkg.api.decision.extraction.DecisionExtractionProvider.SourceBlock;
import com.projectkg.api.decision.repository.DecisionExtractionRunRepository;
import com.projectkg.api.decision.repository.DecisionExtractionRunRepository.ExtractionRunRow;
import com.projectkg.api.notion.repository.ContentBlockRepository;
import com.projectkg.api.notion.repository.ContentBlockRepository.ContentBlockRow;
import com.projectkg.api.notion.repository.SourceDocumentRepository;
import com.projectkg.api.notion.repository.SourceDocumentRepository.SourceDocumentDetailRow;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class DecisionExtractionService {
  private final SourceDocumentRepository sourceDocumentRepository;
  private final ContentBlockRepository contentBlockRepository;
  private final DecisionExtractionRunRepository extractionRunRepository;
  private final DecisionExtractionPersistenceService persistenceService;
  private final DecisionService decisionService;
  private final Optional<DecisionExtractionProvider> extractionProvider;

  public DecisionExtractionService(
      SourceDocumentRepository sourceDocumentRepository,
      ContentBlockRepository contentBlockRepository,
      DecisionExtractionRunRepository extractionRunRepository,
      DecisionExtractionPersistenceService persistenceService,
      DecisionService decisionService,
      Optional<DecisionExtractionProvider> extractionProvider
  ) {
    this.sourceDocumentRepository = sourceDocumentRepository;
    this.contentBlockRepository = contentBlockRepository;
    this.extractionRunRepository = extractionRunRepository;
    this.persistenceService = persistenceService;
    this.decisionService = decisionService;
    this.extractionProvider = extractionProvider;
  }

  public DecisionExtractionResponse extract(long documentId) {
    if (extractionProvider.isEmpty()) {
      return new DecisionExtractionResponse("disabled", null, 0, List.of());
    }

    SourceDocumentDetailRow document = sourceDocumentRepository.findById(documentId)
        .orElseThrow(() -> new IllegalArgumentException("source document not found: " + documentId));
    Optional<ExtractionRunRow> existing = extractionRunRepository
        .findByDocumentAndChecksum(documentId, document.checksum());
    if (existing.isPresent() && "success".equals(existing.get().status())) {
      List<DecisionDto> decisions = decisionService.listByExtractionRunId(existing.get().id());
      return new DecisionExtractionResponse("existing", existing.get().id(), decisions.size(), decisions);
    }
    if (existing.isPresent() && "running".equals(existing.get().status())) {
      throw new DecisionExtractionAlreadyRunningException(documentId);
    }

    long runId;
    if (existing.isPresent()) {
      extractionRunRepository.retryFailed(existing.get().id());
      runId = existing.get().id();
    } else {
      try {
        runId = extractionRunRepository.createRunning(documentId, document.checksum());
      } catch (DecisionExtractionRunRepository.DecisionExtractionAlreadyRunningException ex) {
        throw new DecisionExtractionAlreadyRunningException(documentId);
      }
    }

    try {
      List<ContentBlockRow> blocks = contentBlockRepository.findByDocumentId(documentId);
      List<DecisionCandidate> candidates = validateCandidates(
          extractionProvider.get().extract(
              document.title(),
              blocks.stream().map(block -> new SourceBlock(block.blockId(), block.text())).toList()),
          blocks);
      int extracted = persistenceService.persist(runId, documentId, candidates);
      List<DecisionDto> decisions = decisionService.listByExtractionRunId(runId);
      return new DecisionExtractionResponse("success", runId, extracted, decisions);
    } catch (RuntimeException ex) {
      extractionRunRepository.markFailed(runId, ex.getMessage());
      throw ex;
    }
  }

  static List<DecisionCandidate> validateCandidates(
      List<DecisionCandidate> candidates,
      List<ContentBlockRow> sourceBlocks
  ) {
    Map<String, String> blockText = new LinkedHashMap<>();
    for (ContentBlockRow block : sourceBlocks) {
      blockText.put(block.blockId(), block.text() == null ? "" : block.text());
    }

    List<DecisionCandidate> valid = new ArrayList<>();
    Map<String, Boolean> seenCandidates = new LinkedHashMap<>();
    for (DecisionCandidate candidate : candidates == null ? List.<DecisionCandidate>of() : candidates) {
      if (candidate == null
          || isBlank(candidate.title())
          || isBlank(candidate.discussion())
          || isBlank(candidate.outcome())
          || candidate.confidence() == null
          || candidate.confidence() < 0.0
          || candidate.confidence() > 1.0
          || candidate.evidence() == null
          || candidate.evidence().isEmpty()) {
        continue;
      }

      List<EvidenceCandidate> evidence = new ArrayList<>();
      for (EvidenceCandidate item : candidate.evidence()) {
        if (item == null || isBlank(item.blockId()) || isBlank(item.quote())) {
          continue;
        }
        String source = blockText.get(item.blockId().trim());
        String quote = item.quote().trim();
        if (source == null || !source.contains(quote)) {
          continue;
        }
        evidence.add(new EvidenceCandidate(
            item.blockId().trim(), quote, item.rationale() == null ? null : item.rationale().trim()));
      }
      if (!evidence.isEmpty()) {
        DecisionCandidate normalized = new DecisionCandidate(
            candidate.title().trim(),
            candidate.discussion().trim(),
            candidate.outcome().trim(),
            candidate.confidence(),
            evidence);
        String candidateKey = normalized.title() + "\n" + normalized.outcome() + "\n"
            + normalized.evidence().stream().map(EvidenceCandidate::blockId).sorted().toList();
        if (seenCandidates.putIfAbsent(candidateKey, Boolean.TRUE) == null) {
          valid.add(normalized);
        }
      }
    }
    return valid;
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  public static class DecisionExtractionAlreadyRunningException extends RuntimeException {
    public DecisionExtractionAlreadyRunningException(long documentId) {
      super("Decision extraction is already running for document: " + documentId);
    }
  }
}
