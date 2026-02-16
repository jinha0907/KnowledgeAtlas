package com.projectkg.api.notion.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class TextChunkerTest {

  private final TextChunker textChunker = new TextChunker();

  @Test
  void chunkShouldBeDeterministic() {
    String input = "alpha ".repeat(300);

    List<String> first = textChunker.chunk(input);
    List<String> second = textChunker.chunk(input);

    assertEquals(first, second);
    assertTrue(first.size() > 1);
  }

  @Test
  void blankInputShouldReturnEmptyChunks() {
    assertTrue(textChunker.chunk("   ").isEmpty());
  }
}
