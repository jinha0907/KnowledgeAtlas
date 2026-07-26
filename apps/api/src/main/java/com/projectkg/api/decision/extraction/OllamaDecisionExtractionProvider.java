package com.projectkg.api.decision.extraction;

import com.projectkg.api.ai.OllamaRestClientFactory;
import com.projectkg.api.ai.OllamaSourceBlockLimiter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(prefix = "decision-extraction", name = "provider", havingValue = "ollama")
public class OllamaDecisionExtractionProvider implements DecisionExtractionProvider {
  private static final String SYSTEM_PROMPT = """
      Extract only explicit decisions from the supplied meeting-note blocks. Return JSON with a
      candidates array. Each candidate must include title, discussion, outcome, confidence (0 to 1),
      and evidence. Every evidence item must include blockId, an exact quote copied from that block,
      and a short rationale. Do not infer a decision without an exact supporting quote. Return an
      empty candidates array if no explicit decision exists.
      """;
  private static final Map<String, Object> RESPONSE_SCHEMA = Map.of(
      "type", "object",
      "properties", Map.of("candidates", Map.of("type", "array")),
      "required", List.of("candidates"),
      "additionalProperties", false);

  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final String model;
  private final int maxInputCharacters;
  private final int contextWindow;

  public OllamaDecisionExtractionProvider(
      ObjectMapper objectMapper,
      @Value("${decision-extraction.ollama.base-url:http://localhost:11434}") String baseUrl,
      @Value("${decision-extraction.ollama.model:qwen3:4b}") String model,
      @Value("${ollama.max-input-characters:8000}") int maxInputCharacters,
      @Value("${ollama.context-window:16384}") int contextWindow,
      @Value("${ollama.request-timeout-seconds:300}") int requestTimeoutSeconds
  ) {
    this.objectMapper = objectMapper;
    this.model = model;
    this.maxInputCharacters = maxInputCharacters;
    this.contextWindow = contextWindow;
    this.restClient = OllamaRestClientFactory.create(baseUrl, requestTimeoutSeconds);
  }

  @Override
  public List<DecisionCandidate> extract(String documentTitle, List<SourceBlock> blocks) {
    String content;
    try {
      content = objectMapper.writeValueAsString(Map.of(
          "title", documentTitle == null ? "Untitled" : documentTitle,
          "blocks", OllamaSourceBlockLimiter.limit(blocks, maxInputCharacters)));
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to build decision extraction input", ex);
    }

    String response = restClient.post()
        .uri("/api/chat")
        .body(Map.of(
            "model", model,
            "stream", false,
            "format", RESPONSE_SCHEMA,
            "think", false,
            "options", Map.of("temperature", 0, "num_ctx", contextWindow, "num_predict", 1024),
            "messages", List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", content))))
        .retrieve()
        .body(String.class);
    try {
      JsonNode root = objectMapper.readTree(response == null ? "{}" : response);
      if ("length".equals(root.path("done_reason").asText())) {
        throw new IllegalStateException("Ollama decision extraction reached its output limit");
      }
      JsonNode candidates = objectMapper.readTree(root.path("message").path("content").asText("{\"candidates\":[]}"))
          .path("candidates");
      List<DecisionCandidate> extracted = new ArrayList<>();
      for (JsonNode candidate : candidates) {
        List<EvidenceCandidate> evidence = new ArrayList<>();
        for (JsonNode item : candidate.path("evidence")) {
          evidence.add(new EvidenceCandidate(
              item.path("blockId").asText(""),
              item.path("quote").asText(""),
              item.path("rationale").asText("")));
        }
        extracted.add(new DecisionCandidate(
            candidate.path("title").asText(""),
            candidate.path("discussion").asText(""),
            candidate.path("outcome").asText(""),
            candidate.path("confidence").isNumber() ? candidate.path("confidence").asDouble() : null,
            evidence));
      }
      return extracted;
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to parse Ollama decision extraction response", ex);
    }
  }
}
