package com.projectkg.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "health", description = "Service and database health checks")
public class HealthController {
  private final JdbcTemplate jdbcTemplate;

  public HealthController(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @GetMapping("/health")
  @Operation(summary = "Service health check")
  public Map<String, String> health() {
    return Map.of("status", "ok");
  }

  @GetMapping("/db/health")
  @Operation(summary = "Database connectivity health check")
  public ResponseEntity<Map<String, String>> dbHealth() {
    try {
      Integer ping = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
      if (ping != null && ping == 1) {
        return ResponseEntity.ok(Map.of("status", "ok", "database", "up"));
      }
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(Map.of("status", "error", "database", "unexpected response"));
    } catch (Exception ex) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(Map.of("status", "error", "database", "down"));
    }
  }
}
