package com.projectkg.api.ai;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

public final class OllamaRestClientFactory {
  private static final int CONNECT_TIMEOUT_MILLIS = 10_000;

  private OllamaRestClientFactory() {
  }

  public static RestClient create(String baseUrl, int requestTimeoutSeconds) {
    if (requestTimeoutSeconds <= 0) {
      throw new IllegalArgumentException("ollama.request-timeout-seconds must be positive");
    }
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
    requestFactory.setReadTimeout(Math.multiplyExact(requestTimeoutSeconds, 1_000));
    return RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
  }
}
