package com.projectkg.api.analysis.provider;

import com.projectkg.api.decision.extraction.DecisionExtractionProvider.SourceBlock;
import java.util.List;

public interface DocumentAnalysisProvider {
  AnalysisResult analyze(String documentTitle, List<SourceBlock> blocks);

  record AnalysisResult(String summary, List<String> tags) {}
}
