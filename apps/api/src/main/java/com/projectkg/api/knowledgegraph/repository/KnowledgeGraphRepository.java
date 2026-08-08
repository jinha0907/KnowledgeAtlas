package com.projectkg.api.knowledgegraph.repository;

import com.projectkg.api.embedding.service.EmbeddingIdentity;
import java.util.List;
import java.util.Optional;

public interface KnowledgeGraphRepository {
  void deleteAll();

  int countDocumentsWithEmbeddings(EmbeddingIdentity identity);

  List<SimilarityCandidate> findSparseCandidates(
      EmbeddingIdentity identity, double minimumScore, int maxNeighbors
  );

  Optional<SimilarityEvidence> findBestEvidence(
      long documentIdLow, long documentIdHigh, EmbeddingIdentity identity, int representativeChunkLimit
  );

  void upsert(SimilarityEdge edge);

  List<DocumentNodeRow> findDocumentNodes();

  List<SimilarityEdgeRow> findEdges(EmbeddingIdentity identity, double minimumScore);

  boolean hasEdges();

  record SimilarityCandidate(
      long documentIdLow,
      long documentIdHigh,
      double score,
      String sourceChecksumLow,
      String sourceChecksumHigh
  ) {}

  record SimilarityEvidence(long chunkIdLow, long chunkIdHigh) {}

  record SimilarityEdge(
      long documentIdLow,
      long documentIdHigh,
      double score,
      long chunkIdLow,
      long chunkIdHigh,
      EmbeddingIdentity identity,
      String sourceChecksumLow,
      String sourceChecksumHigh
  ) {}

  record DocumentNodeRow(long documentId, String title, String summary, List<String> tags, int blockCount) {}

  record SimilarityEdgeRow(
      long documentIdLow,
      long documentIdHigh,
      double score,
      long chunkIdLow,
      String blockIdLow,
      String textLow,
      long chunkIdHigh,
      String blockIdHigh,
      String textHigh
  ) {}
}
