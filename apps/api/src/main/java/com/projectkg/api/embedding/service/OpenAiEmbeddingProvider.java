package com.projectkg.api.embedding.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(prefix = "embedding", name = "provider", havingValue = "openai")
public class OpenAiEmbeddingProvider implements EmbeddingProvider {
  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final String apiKey;
  private final String model;
  private final int dimensions;

  public OpenAiEmbeddingProvider(
      ObjectMapper objectMapper,
      @Value("${embedding.openai.base-url:https://api.openai.com/v1}") String baseUrl,
      @Value("${embedding.openai.api-key:}") String apiKey,
      @Value("${embedding.openai.model:text-embedding-3-small}") String model,
      @Value("${embedding.openai.dimensions:1536}") int dimensions
  ) {
    this.objectMapper = objectMapper;
    this.apiKey = apiKey == null ? "" : apiKey.trim();
    this.model = model;
    this.dimensions = dimensions;
    if (dimensions <= 0) {
      throw new IllegalArgumentException("embedding.openai.dimensions must be positive");
    }
    this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    if (this.apiKey.isBlank()) {
      throw new IllegalStateException("embedding.openai.api-key is required when embedding.provider=openai");
    }
  }

  @Override
  public String provider() {
    return "openai";
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
        .uri("/embeddings")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
        .body(Map.of("model", model, "input", inputs, "dimensions", dimensions))
        .retrieve()
        .body(String.class);

    try {
      JsonNode root = objectMapper.readTree(response == null ? "{}" : response);
      List<JsonNode> data = new ArrayList<>();
      for (JsonNode item : root.path("data")) {
        data.add(item);
      }
      data.sort(Comparator.comparingInt(item -> item.path("index").asInt()));

      List<float[]> embeddings = new ArrayList<>();
      for (JsonNode item : data) {
        JsonNode vector = item.path("embedding");
        float[] values = new float[vector.size()];
        for (int i = 0; i < vector.size(); i++) {
          values[i] = (float) vector.get(i).asDouble();
        }
        embeddings.add(values);
      }
      return embeddings;
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to parse OpenAI embeddings response", ex);
    }
  }
}
