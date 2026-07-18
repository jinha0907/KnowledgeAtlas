package com.projectkg.api.analysis.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectkg.api.decision.extraction.DecisionExtractionProvider.SourceBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(prefix = "document-analysis", name = "provider", havingValue = "openai")
public class OpenAiDocumentAnalysisProvider implements DocumentAnalysisProvider {
  private static final String SYSTEM_PROMPT = """
      Summarize the supplied document using only its supplied blocks. Return JSON with a summary
      and tags array. The summary must be a concise factual overview, no more than 800 characters.
      tags must contain one to eight short, specific topic labels. Do not invent facts, decisions,
      people, or tags unsupported by the document. Return plain strings only.
      """;

  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final String apiKey;
  private final String model;

  public OpenAiDocumentAnalysisProvider(
      ObjectMapper objectMapper,
      @Value("${document-analysis.openai.base-url:https://api.openai.com/v1}") String baseUrl,
      @Value("${document-analysis.openai.api-key:}") String apiKey,
      @Value("${document-analysis.openai.model:gpt-4.1-mini}") String model
  ) {
    this.objectMapper = objectMapper;
    this.apiKey = apiKey == null ? "" : apiKey.trim();
    this.model = model;
    this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    if (this.apiKey.isBlank()) {
      throw new IllegalStateException(
          "document-analysis.openai.api-key is required when document-analysis.provider=openai");
    }
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
      JsonNode analysis = objectMapper.readTree(result.isBlank() ? "{}" : result);
      List<String> tags = new ArrayList<>();
      for (JsonNode tag : analysis.path("tags")) {
        tags.add(tag.asText(""));
      }
      return new AnalysisResult(analysis.path("summary").asText(""), tags);
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to parse document analysis response", ex);
    }
  }
}
