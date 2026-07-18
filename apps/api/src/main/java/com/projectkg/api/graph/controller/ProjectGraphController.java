package com.projectkg.api.graph.controller;

import com.projectkg.api.graph.dto.ProjectGraphDto;
import com.projectkg.api.graph.service.ProjectGraphService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/project-graph")
@Tag(name = "graph", description = "Evidence-backed project graph read API")
public class ProjectGraphController {
  private final ProjectGraphService projectGraphService;

  public ProjectGraphController(ProjectGraphService projectGraphService) {
    this.projectGraphService = projectGraphService;
  }

  @GetMapping
  @Operation(summary = "Get deterministic document, decision, and evidence graph")
  public ResponseEntity<ProjectGraphDto> getGraph() {
    return ResponseEntity.ok(projectGraphService.getGraph());
  }
}
