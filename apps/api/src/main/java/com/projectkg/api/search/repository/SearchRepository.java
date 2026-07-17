package com.projectkg.api.search.repository;

import java.util.List;

public interface SearchRepository {
  List<SearchRow> searchByKeyword(String query, int topK);

  default List<SearchRow> searchByHybrid(String query, float[] queryEmbedding, int topK) {
    return searchByKeyword(query, topK);
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
