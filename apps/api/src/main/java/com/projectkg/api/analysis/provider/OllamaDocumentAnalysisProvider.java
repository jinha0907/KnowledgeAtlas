package com.projectkg.api.analysis.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectkg.api.decision.extraction.DecisionExtractionProvider.SourceBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(prefix = "document-analysis", name = "provider", havingValue = "ollama")
public class OllamaDocumentAnalysisProvider implements DocumentAnalysisProvider {
  private static final String SYSTEM_PROMPT = """
      Summarize the supplied document using only its supplied blocks. Return JSON with a summary
      and tags array. The summary must be a concise factual overview, no more than 800 characters.
      tags must contain one to eight short, specific topic labels. Do not invent facts, decisions,
      people, or tags unsupported by the document. Return plain strings only.
      """;

  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final String model;

  public OllamaDocumentAnalysisProvider(
      ObjectMapper objectMapper,
      @Value("${document-analysis.ollama.base-url:http://localhost:11434}") String baseUrl,
      @Value("${document-analysis.ollama.model:qwen3:4b}") String model
  ) {
    this.objectMapper = objectMapper;
    this.model = model;
    this.restClient = RestClient.builder().baseUrl(baseUrl).build();
  }

  @Override
  public AnalysisResult analyze(String documentTitle, List<SourceBlock> blocks) {
    String content;
    try {
      content = objectMapper.writeValueAsString(Map.of(
          "title", documentTitle == null ? "Untitled" : documentTitle,
          "blocks", blocks));
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to build document analysis input", ex);
    }

    String response = restClient.post()
        .uri("/api/chat")
        .body(Map.of(
            "model", model,
            "stream", false,
            "format", "json",
            "options", Map.of("temperature", 0),
            "messages", List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", content))))
        .retrieve()
        .body(String.class);
    try {
      JsonNode root = objectMapper.readTree(response == null ? "{}" : response);
      JsonNode analysis = objectMapper.readTree(root.path("message").path("content").asText("{}"));
      List<String> tags = new ArrayList<>();
      for (JsonNode tag : analysis.path("tags")) {
        tags.add(tag.asText(""));
      }
      return new AnalysisResult(analysis.path("summary").asText(""), tags);
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to parse Ollama document analysis response", ex);
    }
  }
}
