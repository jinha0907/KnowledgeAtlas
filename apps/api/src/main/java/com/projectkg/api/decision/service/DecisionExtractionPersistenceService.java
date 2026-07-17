package com.projectkg.api.decision.service;

import com.projectkg.api.decision.extraction.DecisionExtractionProvider.DecisionCandidate;
import com.projectkg.api.decision.extraction.DecisionExtractionProvider.EvidenceCandidate;
import com.projectkg.api.decision.repository.DecisionEvidenceRepository;
import com.projectkg.api.decision.repository.DecisionExtractionRunRepository;
import com.projectkg.api.decision.repository.DecisionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DecisionExtractionPersistenceService {
  private final DecisionRepository decisionRepository;
  private final DecisionEvidenceRepository decisionEvidenceRepository;
  private final DecisionExtractionRunRepository extractionRunRepository;

  public DecisionExtractionPersistenceService(
      DecisionRepository decisionRepository,
      DecisionEvidenceRepository decisionEvidenceRepository,
      DecisionExtractionRunRepository extractionRunRepository
  ) {
    this.decisionRepository = decisionRepository;
    this.decisionEvidenceRepository = decisionEvidenceRepository;
    this.extractionRunRepository = extractionRunRepository;
  }

  @Transactional
  public int persist(long runId, long documentId, List<DecisionCandidate> candidates) {
    for (DecisionCandidate candidate : candidates) {
      long decisionId = decisionRepository.createExtracted(
          candidate.title(),
          candidate.discussion(),
          candidate.outcome(),
          candidate.confidence(),
          runId);
      for (EvidenceCandidate evidence : candidate.evidence()) {
        decisionEvidenceRepository.create(
            decisionId,
            documentId,
            evidence.blockId(),
            evidence.quote(),
            evidence.rationale());
      }
    }
    extractionRunRepository.markSuccess(runId, candidates.size());
    return candidates.size();
  }
}
