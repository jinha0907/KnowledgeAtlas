package com.projectkg.api.search.repository;

import com.projectkg.api.embedding.service.EmbeddingIdentity;
import java.util.List;

public interface SearchRepository {
  List<SearchRow> searchByKeyword(String query, int topK);

  default List<SearchRow> searchByHybrid(
      String query, float[] queryEmbedding, EmbeddingIdentity identity, int topK
  ) {
    return searchByKeyword(query, topK);
  }

  default List<EmbeddingIdentity> findEmbeddingIdentities() {
    return List.of();
  }

  public record SearchRow(
      long chunkId,
      long documentId,
      String blockId,
      String title,
      String text,
      double score
  ) {}
}
