package com.projectkg.api.decision.controller;

import com.projectkg.api.decision.dto.AddDecisionEvidenceRequest;
import com.projectkg.api.decision.dto.CreateDecisionRequest;
import com.projectkg.api.decision.dto.DecisionDto;
import com.projectkg.api.decision.dto.DecisionEvidenceDto;
import com.projectkg.api.decision.dto.UpdateDecisionStatusRequest;
import com.projectkg.api.decision.service.DecisionService;
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
public class DecisionController {
  private final DecisionService decisionService;

  public DecisionController(DecisionService decisionService) {
    this.decisionService = decisionService;
  }

  @PostMapping
  public ResponseEntity<DecisionDto> create(@RequestBody CreateDecisionRequest request) {
    return ResponseEntity.ok(decisionService.create(request));
  }

  @GetMapping
  public ResponseEntity<List<DecisionDto>> list() {
    return ResponseEntity.ok(decisionService.list());
  }

  @GetMapping("/{id}")
  public ResponseEntity<DecisionDto> getById(@PathVariable long id) {
    return ResponseEntity.ok(decisionService.getById(id));
  }

  @PatchMapping("/{id}/status")
  public ResponseEntity<DecisionDto> updateStatus(
      @PathVariable long id,
      @RequestBody UpdateDecisionStatusRequest request
  ) {
    return ResponseEntity.ok(decisionService.updateStatus(id, request));
  }

  @PostMapping("/{id}/evidence")
  public ResponseEntity<DecisionEvidenceDto> addEvidence(
      @PathVariable long id,
      @RequestBody AddDecisionEvidenceRequest request
  ) {
    return ResponseEntity.ok(decisionService.addEvidence(id, request));
  }
}
