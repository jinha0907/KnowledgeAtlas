package com.projectkg.api.decision.extraction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(prefix = "decision-extraction", name = "provider", havingValue = "openai")
public class OpenAiDecisionExtractionProvider implements DecisionExtractionProvider {
  private static final String SYSTEM_PROMPT = """
      Extract only explicit decisions from the supplied meeting-note blocks. Return JSON with a
      candidates array. Each candidate must include title, discussion, outcome, confidence (0 to 1),
      and evidence. Every evidence item must include blockId, an exact quote copied from that block,
      and a short rationale. Do not infer a decision without an exact supporting quote. Return an
      empty candidates array if no explicit decision exists.
      """;

  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final String apiKey;
  private final String model;

  public OpenAiDecisionExtractionProvider(
      ObjectMapper objectMapper,
      @Value("${decision-extraction.openai.base-url:https://api.openai.com/v1}") String baseUrl,
      @Value("${decision-extraction.openai.api-key:}") String apiKey,
      @Value("${decision-extraction.openai.model:gpt-4.1-mini}") String model
  ) {
    this.objectMapper = objectMapper;
    this.apiKey = apiKey == null ? "" : apiKey.trim();
    this.model = model;
    this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    if (this.apiKey.isBlank()) {
      throw new IllegalStateException(
          "decision-extraction.openai.api-key is required when decision-extraction.provider=openai");
    }
  }

  @Override
  public List<DecisionCandidate> extract(String documentTitle, List<SourceBlock> blocks) {
    String content;
    try {
      content = objectMapper.writeValueAsString(Map.of(
          "title", documentTitle == null ? "Untitled" : documentTitle,
          "blocks", blocks));
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to build decision extraction input", ex);
    }

    String response = restClient.post()
        .uri("/chat/completions")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
        .body(Map.of(
            "model", model,
            "temperature", 0,
            "response_format", Map.of("type", "json_object"),
            "messages", List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", content))))
        .retrieve()
        .body(String.class);

    try {
      JsonNode root = objectMapper.readTree(response == null ? "{}" : response);
      String result = root.path("choices").path(0).path("message").path("content").asText("");
      JsonNode candidates = objectMapper.readTree(result.isBlank() ? "{\"candidates\":[]}" : result)
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
      throw new IllegalStateException("Failed to parse decision extraction response", ex);
    }
  }
}
