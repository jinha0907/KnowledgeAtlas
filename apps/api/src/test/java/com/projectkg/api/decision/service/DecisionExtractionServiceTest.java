package com.projectkg.api.decision.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.projectkg.api.decision.dto.DecisionExtractionResponse;
import com.projectkg.api.decision.extraction.DecisionExtractionProvider.DecisionCandidate;
import com.projectkg.api.decision.extraction.DecisionExtractionProvider.EvidenceCandidate;
import com.projectkg.api.notion.repository.ContentBlockRepository.ContentBlockRow;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DecisionExtractionServiceTest {

  @Test
  void shouldKeepOnlyCandidatesWithVerifiableEvidence() {
    List<ContentBlockRow> source = List.of(new ContentBlockRow("block-1", "Deploy on Friday", "/1"));
    DecisionCandidate validCandidate = candidate("block-1", "Deploy on Friday", 0.9);
    List<DecisionCandidate> candidates = List.of(
        validCandidate,
        validCandidate,
        candidate("missing", "Invented quote", 0.8),
        candidate("block-1", "Deploy on Friday", 1.5));

    List<DecisionCandidate> valid = DecisionExtractionService.validateCandidates(candidates, source);

    assertEquals(1, valid.size());
    assertEquals("Deploy on Friday", valid.getFirst().evidence().getFirst().quote());
  }

  @Test
  void shouldReturnDisabledWithoutLoadingOrSendingDocumentContent() {
    DecisionExtractionService service = new DecisionExtractionService(
        null, null, null, null, null, Optional.empty());

    DecisionExtractionResponse response = service.extract(42L);

    assertEquals("disabled", response.status());
    assertEquals(0, response.extractedDecisions());
  }

  private DecisionCandidate candidate(String blockId, String quote, double confidence) {
    return new DecisionCandidate(
        "Deployment decision",
        "The team discussed the release schedule.",
        "Deploy on Friday.",
        confidence,
        List.of(new EvidenceCandidate(blockId, quote, "Explicit meeting outcome")));
  }
}
