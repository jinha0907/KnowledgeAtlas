package com.projectkg.api.graph.service;

import com.projectkg.api.graph.dto.GraphEdgeDto;
import com.projectkg.api.graph.dto.GraphNodeDto;
import com.projectkg.api.graph.dto.ProjectGraphDto;
import com.projectkg.api.graph.repository.ProjectGraphRepository;
import com.projectkg.api.graph.repository.ProjectGraphRepository.DecisionRow;
import com.projectkg.api.graph.repository.ProjectGraphRepository.DocumentRow;
import com.projectkg.api.graph.repository.ProjectGraphRepository.EvidenceRow;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProjectGraphService {
  private final ProjectGraphRepository projectGraphRepository;

  public ProjectGraphService(ProjectGraphRepository projectGraphRepository) {
    this.projectGraphRepository = projectGraphRepository;
  }

  public ProjectGraphDto getGraph() {
    return assemble(
        projectGraphRepository.findDocuments(),
        projectGraphRepository.findDecisions(),
        projectGraphRepository.findEvidence());
  }

  static ProjectGraphDto assemble(
      List<DocumentRow> documents,
      List<DecisionRow> decisions,
      List<EvidenceRow> evidenceRows
  ) {
    List<GraphNodeDto> nodes = new ArrayList<>();
    List<GraphEdgeDto> edges = new ArrayList<>();

    for (DocumentRow document : documents.stream().sorted(Comparator.comparingLong(DocumentRow::id)).toList()) {
      nodes.add(new GraphNodeDto(
          documentId(document.id()), "document", display(document.title(), "Untitled document"), null,
          document.id(), null, null, null));
    }
    for (DecisionRow decision : decisions.stream().sorted(Comparator.comparingLong(DecisionRow::id)).toList()) {
      nodes.add(new GraphNodeDto(
          decisionId(decision.id()), "decision", display(decision.title(), "Untitled decision"),
          decision.status(), null, decision.id(), null, null));
    }
    for (EvidenceRow evidence : evidenceRows.stream().sorted(Comparator.comparingLong(EvidenceRow::id)).toList()) {
      nodes.add(new GraphNodeDto(
          evidenceId(evidence.id()), "evidence", evidenceLabel(evidence), null,
          evidence.documentId(), evidence.decisionId(), evidence.id(), evidence.blockId()));
      edges.add(new GraphEdgeDto(
          "edge-decision-" + evidence.id(), decisionId(evidence.decisionId()), evidenceId(evidence.id()),
          "supports", evidence.id(), evidence.blockId()));
      edges.add(new GraphEdgeDto(
          "edge-document-" + evidence.id(), evidenceId(evidence.id()), documentId(evidence.documentId()),
          "sources", evidence.id(), evidence.blockId()));
    }
    return new ProjectGraphDto(List.copyOf(nodes), List.copyOf(edges));
  }

  private static String documentId(long id) {
    return "document-" + id;
  }

  private static String decisionId(long id) {
    return "decision-" + id;
  }

  private static String evidenceId(long id) {
    return "evidence-" + id;
  }

  private static String display(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }

  private static String evidenceLabel(EvidenceRow evidence) {
    String quote = display(evidence.quote(), "Stored evidence");
    String compact = quote.length() > 54 ? quote.substring(0, 51) + "..." : quote;
    return "Block " + display(evidence.blockId(), "unknown") + ": " + compact;
  }
}
