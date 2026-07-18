package com.projectkg.api.controller;

import com.projectkg.api.notion.repository.JdbcSyncJobRunRepository.SyncAlreadyRunningException;
import com.projectkg.api.decision.service.DecisionExtractionService.DecisionExtractionAlreadyRunningException;
import com.projectkg.api.analysis.service.DocumentAnalysisService.DocumentAnalysisAlreadyRunningException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(Map.of("error", ex.getMessage()));
  }

  @ExceptionHandler(SyncAlreadyRunningException.class)
  public ResponseEntity<Map<String, String>> handleSyncAlreadyRunning(SyncAlreadyRunningException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(Map.of("error", ex.getMessage()));
  }

  @ExceptionHandler(DecisionExtractionAlreadyRunningException.class)
  public ResponseEntity<Map<String, String>> handleDecisionExtractionAlreadyRunning(
      DecisionExtractionAlreadyRunningException ex
  ) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(Map.of("error", ex.getMessage()));
  }

  @ExceptionHandler(DocumentAnalysisAlreadyRunningException.class)
  public ResponseEntity<Map<String, String>> handleDocumentAnalysisAlreadyRunning(
      DocumentAnalysisAlreadyRunningException ex
  ) {
    return ResponseEntity.status(409).body(Map.of("error", ex.getMessage()));
  }
}
