package com.projectkg.api.decision.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DecisionStatusPolicyTest {

  @Test
  void shouldAllowValidTransitions() {
    assertDoesNotThrow(() -> DecisionStatusPolicy.validateTransition("proposed", "accepted"));
    assertDoesNotThrow(() -> DecisionStatusPolicy.validateTransition("accepted", "obsolete"));
    assertDoesNotThrow(() -> DecisionStatusPolicy.validateTransition("accepted", "accepted"));
  }

  @Test
  void shouldRejectInvalidTransitions() {
    assertThrows(IllegalArgumentException.class,
        () -> DecisionStatusPolicy.validateTransition("proposed", "obsolete"));
    assertThrows(IllegalArgumentException.class,
        () -> DecisionStatusPolicy.validateTransition("obsolete", "accepted"));
  }
}
