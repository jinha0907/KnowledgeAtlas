package com.projectkg.api.embedding.service;

import java.util.List;

public interface EmbeddingProvider {
  String model();

  List<float[]> embed(List<String> inputs);
}
