package com.projectkg.api.notion.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TextChunker {
  private static final int CHUNK_SIZE = 500;
  private static final int CHUNK_OVERLAP = 50;

  public List<String> chunk(String input) {
    if (input == null || input.isBlank()) {
      return List.of();
    }

    String text = input.trim();
    List<String> chunks = new ArrayList<>();
    int start = 0;

    while (start < text.length()) {
      int end = Math.min(text.length(), start + CHUNK_SIZE);

      if (end < text.length()) {
        int lastSpace = text.lastIndexOf(' ', end);
        if (lastSpace > start + (CHUNK_SIZE / 2)) {
          end = lastSpace;
        }
      }

      String chunk = text.substring(start, end).trim();
      if (!chunk.isEmpty()) {
        chunks.add(chunk);
      }

      if (end >= text.length()) {
        break;
      }

      int nextStart = Math.max(end - CHUNK_OVERLAP, start + 1);
      start = nextStart;
    }

    return chunks;
  }
}
