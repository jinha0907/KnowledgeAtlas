package com.projectkg.api.embedding.service;

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
@ConditionalOnProperty(prefix = "embedding", name = "provider", havingValue = "ollama")
public class OllamaEmbeddingProvider implements EmbeddingProvider {
  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final String model;
  private final int dimensions;

  public OllamaEmbeddingProvider(
      ObjectMapper objectMapper,
      @Value("${embedding.ollama.base-url:http://localhost:11434}") String baseUrl,
      @Value("${embedding.ollama.model:bge-m3}") String model,
      @Value("${embedding.ollama.dimensions:1024}") int dimensions
  ) {
    if (dimensions <= 0) {
      throw new IllegalArgumentException("embedding.ollama.dimensions must be positive");
    }
    this.objectMapper = objectMapper;
    this.model = model;
    this.dimensions = dimensions;
    this.restClient = RestClient.builder().baseUrl(baseUrl).build();
  }

  @Override
  public String provider() {
    return "ollama";
  }

  @Override
  public String model() {
    return model;
  }

  @Override
  public int dimensions() {
    return dimensions;
  }

  @Override
  public List<float[]> embed(List<String> inputs) {
    if (inputs == null || inputs.isEmpty()) {
      return List.of();
    }
    String response = restClient.post()
        .uri("/api/embed")
        .body(Map.of("model", model, "input", inputs))
        .retrieve()
        .body(String.class);
    try {
      List<float[]> embeddings = new ArrayList<>();
      for (JsonNode vector : objectMapper.readTree(response == null ? "{}" : response).path("embeddings")) {
        float[] values = new float[vector.size()];
        for (int i = 0; i < vector.size(); i++) {
          values[i] = (float) vector.get(i).asDouble();
        }
        embeddings.add(values);
      }
      return embeddings;
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to parse Ollama embedding response", ex);
    }
  }
}
