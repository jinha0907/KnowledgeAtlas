package com.projectkg.api.decision.extraction;

import java.util.List;

public interface DecisionExtractionProvider {
  List<DecisionCandidate> extract(String documentTitle, List<SourceBlock> blocks);

  record SourceBlock(String blockId, String text) {}

  record DecisionCandidate(
      String title,
      String discussion,
      String outcome,
      Double confidence,
      List<EvidenceCandidate> evidence
  ) {}

  record EvidenceCandidate(String blockId, String quote, String rationale) {}
}
