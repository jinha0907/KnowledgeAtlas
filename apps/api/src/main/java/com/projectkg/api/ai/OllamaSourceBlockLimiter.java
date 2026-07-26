package com.projectkg.api.ai;

import com.projectkg.api.decision.extraction.DecisionExtractionProvider.SourceBlock;
import java.util.ArrayList;
import java.util.List;

public final class OllamaSourceBlockLimiter {
  private OllamaSourceBlockLimiter() {
  }

  /**
   * Keeps a deterministic, whole-block prefix so a local model never receives an unbounded prompt.
   */
  public static List<SourceBlock> limit(List<SourceBlock> blocks, int maxInputCharacters) {
    if (maxInputCharacters <= 0) {
      throw new IllegalArgumentException("ollama.max-input-characters must be positive");
    }

    List<SourceBlock> limited = new ArrayList<>();
    int usedCharacters = 0;
    for (SourceBlock block : blocks == null ? List.<SourceBlock>of() : blocks) {
      String text = block.text() == null ? "" : block.text();
      if (usedCharacters + text.length() > maxInputCharacters) {
        break;
      }
      limited.add(new SourceBlock(block.blockId(), text));
      usedCharacters += text.length();
    }
    return limited;
  }
}
