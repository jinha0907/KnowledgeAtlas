package com.projectkg.api.decision.service;

import com.projectkg.api.decision.dto.AddDecisionEvidenceRequest;
import com.projectkg.api.decision.dto.CreateDecisionRequest;
import com.projectkg.api.decision.dto.DecisionDto;
import com.projectkg.api.decision.dto.DecisionEvidenceDto;
import com.projectkg.api.decision.dto.UpdateDecisionStatusRequest;
import com.projectkg.api.decision.repository.DecisionEvidenceRepository;
import com.projectkg.api.decision.repository.DecisionRepository;
import com.projectkg.api.decision.repository.DecisionEvidenceRepository.DecisionEvidenceRow;
import com.projectkg.api.decision.repository.DecisionRepository.DecisionRow;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DecisionService {
  private final DecisionRepository decisionRepository;
  private final DecisionEvidenceRepository decisionEvidenceRepository;

  public DecisionService(
      DecisionRepository decisionRepository,
      DecisionEvidenceRepository decisionEvidenceRepository
  ) {
    this.decisionRepository = decisionRepository;
    this.decisionEvidenceRepository = decisionEvidenceRepository;
  }

  @Transactional
  public DecisionDto create(CreateDecisionRequest request) {
    if (request == null || request.title() == null || request.title().isBlank()) {
      throw new IllegalArgumentException("title is required");
    }
    if (request.outcome() == null || request.outcome().isBlank()) {
      throw new IllegalArgumentException("outcome is required");
    }

    String status = request.status() == null || request.status().isBlank() ? "proposed" : request.status();
    DecisionStatusPolicy.validateStatusValue(status);

    long id = decisionRepository.create(
        request.title().trim(),
        status,
        request.outcome().trim(),
        request.supersedesDecisionId());

    return getById(id);
  }

  public List<DecisionDto> list() {
    return decisionRepository.findAll().stream().map(this::toDecisionDto).toList();
  }

  public DecisionDto getById(long id) {
    DecisionRow decision = decisionRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("decision not found: " + id));
    return toDecisionDto(decision);
  }

  @Transactional
  public DecisionDto updateStatus(long id, UpdateDecisionStatusRequest request) {
    if (request == null || request.status() == null || request.status().isBlank()) {
      throw new IllegalArgumentException("status is required");
    }

    DecisionRow decision = decisionRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("decision not found: " + id));

    String nextStatus = request.status().trim();
    DecisionStatusPolicy.validateTransition(decision.status(), nextStatus);

    decisionRepository.updateStatus(id, nextStatus, request.supersedesDecisionId(), Instant.now());
    return getById(id);
  }

  @Transactional
  public DecisionEvidenceDto addEvidence(long decisionId, AddDecisionEvidenceRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("request is required");
    }
    if (request.blockId() == null || request.blockId().isBlank()) {
      throw new IllegalArgumentException("blockId is required");
    }
    if (request.quote() == null || request.quote().isBlank()) {
      throw new IllegalArgumentException("quote is required");
    }

    decisionRepository.findById(decisionId)
        .orElseThrow(() -> new IllegalArgumentException("decision not found: " + decisionId));

    long evidenceId = decisionEvidenceRepository.create(
        decisionId,
        request.documentId(),
        request.blockId().trim(),
        request.quote().trim(),
        request.rationale());

    return new DecisionEvidenceDto(
        evidenceId,
        decisionId,
        request.documentId(),
        request.blockId().trim(),
        request.quote().trim(),
        request.rationale());
  }

  private DecisionDto toDecisionDto(DecisionRow decision) {
    List<DecisionEvidenceDto> evidence = decisionEvidenceRepository.findByDecisionId(decision.id()).stream()
        .map(this::toDecisionEvidenceDto)
        .toList();

    return new DecisionDto(
        decision.id(),
        decision.title(),
        decision.status(),
        decision.outcome(),
        decision.supersedesDecisionId(),
        decision.createdAt(),
        decision.updatedAt(),
        evidence);
  }

  private DecisionEvidenceDto toDecisionEvidenceDto(DecisionEvidenceRow row) {
    return new DecisionEvidenceDto(
        row.id(),
        row.decisionId(),
        row.documentId(),
        row.blockId(),
        row.quote(),
        row.rationale());
  }
}
