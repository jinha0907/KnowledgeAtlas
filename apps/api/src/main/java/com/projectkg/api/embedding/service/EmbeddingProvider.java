package com.projectkg.api.embedding.service;

import java.util.List;

public interface EmbeddingProvider {
  String provider();

  String model();

  int dimensions();

  List<float[]> embed(List<String> inputs);
}
