package com.projectkg.api.graph.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.projectkg.api.graph.dto.ProjectGraphDto;
import com.projectkg.api.graph.repository.ProjectGraphRepository.DecisionRow;
import com.projectkg.api.graph.repository.ProjectGraphRepository.DocumentRow;
import com.projectkg.api.graph.repository.ProjectGraphRepository.EvidenceRow;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProjectGraphServiceTest {

  @Test
  void shouldBuildDeterministicEvidenceOnlyPaths() {
    ProjectGraphDto graph = ProjectGraphService.assemble(
        List.of(new DocumentRow(2L, "Planning")),
        List.of(new DecisionRow(7L, "Ship Friday", "proposed")),
        List.of(new EvidenceRow(11L, 7L, 2L, "block-a", "The team agreed to ship Friday.")));

    assertEquals(List.of("document-2", "decision-7", "evidence-11"),
        graph.nodes().stream().map(node -> node.id()).toList());
    assertEquals(List.of("edge-decision-11", "edge-document-11"),
        graph.edges().stream().map(edge -> edge.id()).toList());
    assertEquals("decision-7", graph.edges().getFirst().sourceId());
    assertEquals("evidence-11", graph.edges().getFirst().targetId());
    assertEquals("document-2", graph.edges().get(1).targetId());
  }

  @Test
  void shouldKeepPersistedNodesWithoutInventingEdges() {
    ProjectGraphDto graph = ProjectGraphService.assemble(
        List.of(new DocumentRow(1L, "Notes")),
        List.of(new DecisionRow(3L, "Unlinked", "accepted")),
        List.of());

    assertEquals(2, graph.nodes().size());
    assertEquals(List.of(), graph.edges());
  }
}
