package com.projectkg.api.decision.controller;

import com.projectkg.api.decision.dto.AddDecisionEvidenceRequest;
import com.projectkg.api.decision.dto.CreateDecisionRequest;
import com.projectkg.api.decision.dto.DecisionDto;
import com.projectkg.api.decision.dto.DecisionEvidenceDto;
import com.projectkg.api.decision.dto.UpdateDecisionStatusRequest;
import com.projectkg.api.decision.service.DecisionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/decisions")
@Tag(name = "decision", description = "Decision lifecycle and evidence APIs")
public class DecisionController {
  private final DecisionService decisionService;

  public DecisionController(DecisionService decisionService) {
    this.decisionService = decisionService;
  }

  @PostMapping
  @Operation(summary = "Create a decision")
  public ResponseEntity<DecisionDto> create(@RequestBody CreateDecisionRequest request) {
    return ResponseEntity.ok(decisionService.create(request));
  }

  @GetMapping
  @Operation(summary = "List decisions")
  public ResponseEntity<List<DecisionDto>> list() {
    return ResponseEntity.ok(decisionService.list());
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get decision by id")
  public ResponseEntity<DecisionDto> getById(@PathVariable long id) {
    return ResponseEntity.ok(decisionService.getById(id));
  }

  @PatchMapping("/{id}/status")
  @Operation(summary = "Update decision status")
  public ResponseEntity<DecisionDto> updateStatus(
      @PathVariable long id,
      @RequestBody UpdateDecisionStatusRequest request
  ) {
    return ResponseEntity.ok(decisionService.updateStatus(id, request));
  }

  @PostMapping("/{id}/evidence")
  @Operation(summary = "Attach evidence to decision")
  public ResponseEntity<DecisionEvidenceDto> addEvidence(
      @PathVariable long id,
      @RequestBody AddDecisionEvidenceRequest request
  ) {
    return ResponseEntity.ok(decisionService.addEvidence(id, request));
  }
}
