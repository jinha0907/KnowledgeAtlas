package com.projectkg.api.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.projectkg.api.decision.extraction.DecisionExtractionProvider.SourceBlock;
import java.util.List;
import org.junit.jupiter.api.Test;

class OllamaSourceBlockLimiterTest {

  @Test
  void shouldKeepWholeBlocksInSourceOrderWithinTheCharacterBudget() {
    List<SourceBlock> result = OllamaSourceBlockLimiter.limit(List.of(
        new SourceBlock("a", "four"),
        new SourceBlock("b", "five!"),
        new SourceBlock("c", "ignored")), 9);

    assertEquals(List.of(
        new SourceBlock("a", "four"),
        new SourceBlock("b", "five!")), result);
  }

  @Test
  void shouldRejectANonPositiveCharacterBudget() {
    assertThrows(IllegalArgumentException.class,
        () -> OllamaSourceBlockLimiter.limit(List.of(), 0));
  }
}
