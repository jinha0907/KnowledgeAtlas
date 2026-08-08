package com.projectkg.api.knowledgegraph.service;

import com.projectkg.api.embedding.dto.EmbeddingIdentityDto;
import com.projectkg.api.embedding.service.EmbeddingIdentity;
import com.projectkg.api.embedding.service.EmbeddingProvider;
import com.projectkg.api.knowledgegraph.dto.KnowledgeGraphCitationDto;
import com.projectkg.api.knowledgegraph.dto.KnowledgeGraphDto;
import com.projectkg.api.knowledgegraph.dto.KnowledgeGraphEdgeDto;
import com.projectkg.api.knowledgegraph.dto.KnowledgeGraphNodeDto;
import com.projectkg.api.knowledgegraph.dto.KnowledgeGraphRebuildResponse;
import com.projectkg.api.knowledgegraph.repository.KnowledgeGraphRepository;
import com.projectkg.api.knowledgegraph.repository.KnowledgeGraphRepository.DocumentNodeRow;
import com.projectkg.api.knowledgegraph.repository.KnowledgeGraphRepository.SimilarityCandidate;
import com.projectkg.api.knowledgegraph.repository.KnowledgeGraphRepository.SimilarityEdge;
import com.projectkg.api.knowledgegraph.repository.KnowledgeGraphRepository.SimilarityEdgeRow;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KnowledgeGraphService {
  private static final int DEFAULT_MAX_NEIGHBORS = 4;
  private static final int REPRESENTATIVE_CHUNK_LIMIT = 64;

  private final KnowledgeGraphRepository knowledgeGraphRepository;
  private final Optional<EmbeddingProvider> embeddingProvider;
  private final double rebuildMinimumScore;

  public KnowledgeGraphService(
      KnowledgeGraphRepository knowledgeGraphRepository,
      Optional<EmbeddingProvider> embeddingProvider,
      @Value("${knowledge-graph.rebuild-minimum-score:0.35}") double rebuildMinimumScore
  ) {
    this.knowledgeGraphRepository = knowledgeGraphRepository;
    this.embeddingProvider = embeddingProvider;
    this.rebuildMinimumScore = rebuildMinimumScore;
  }

  @Transactional
  public KnowledgeGraphRebuildResponse rebuild() {
    if (embeddingProvider.isEmpty()) {
      return new KnowledgeGraphRebuildResponse("disabled", 0, 0);
    }

    EmbeddingIdentity identity = activeIdentity();
    int documents = knowledgeGraphRepository.countDocumentsWithEmbeddings(identity);
    knowledgeGraphRepository.deleteAll();
    if (documents < 2) {
      return new KnowledgeGraphRebuildResponse("success", documents, 0);
    }

    int edges = 0;
    for (SimilarityCandidate candidate : knowledgeGraphRepository.findSparseCandidates(
        identity, rebuildMinimumScore, DEFAULT_MAX_NEIGHBORS)) {
      Optional<KnowledgeGraphRepository.SimilarityEvidence> evidence = knowledgeGraphRepository
          .findBestEvidence(
              candidate.documentIdLow(), candidate.documentIdHigh(), identity, REPRESENTATIVE_CHUNK_LIMIT);
      if (evidence.isEmpty()) {
        continue;
      }
      knowledgeGraphRepository.upsert(new SimilarityEdge(
          candidate.documentIdLow(), candidate.documentIdHigh(), candidate.score(),
          evidence.get().chunkIdLow(), evidence.get().chunkIdHigh(), identity,
          candidate.sourceChecksumLow(), candidate.sourceChecksumHigh()));
      edges++;
    }
    return new KnowledgeGraphRebuildResponse("success", documents, edges);
  }

  public KnowledgeGraphDto getGraph(Double requestedMinimumScore) {
    double minimumScore = normalizeMinimumScore(requestedMinimumScore);
    List<DocumentNodeRow> documents = knowledgeGraphRepository.findDocumentNodes();
    if (embeddingProvider.isEmpty()) {
      return assemble("disabled", null, documents, List.of());
    }

    EmbeddingIdentity identity = activeIdentity();
    List<SimilarityEdgeRow> edges = knowledgeGraphRepository.findEdges(identity, minimumScore);
    String status = edges.isEmpty() && knowledgeGraphRepository.hasEdges() ? "rebuild_required" : "ready";
    return assemble(status, identity, documents, edges);
  }

  static KnowledgeGraphDto assemble(
      String status,
      EmbeddingIdentity identity,
      List<DocumentNodeRow> documents,
      List<SimilarityEdgeRow> similarityEdges
  ) {
    List<KnowledgeGraphNodeDto> nodes = documents.stream()
        .sorted(Comparator.comparingLong(DocumentNodeRow::documentId))
        .map(document -> new KnowledgeGraphNodeDto(
            "document-" + document.documentId(),
            document.documentId(),
            display(document.title(), "Untitled document"),
            document.summary(),
            document.tags() == null ? List.of() : List.copyOf(document.tags()),
            document.blockCount()))
        .toList();
    List<KnowledgeGraphEdgeDto> edges = new ArrayList<>();
    for (SimilarityEdgeRow edge : similarityEdges.stream()
        .sorted(Comparator.comparingDouble(SimilarityEdgeRow::score).reversed()
            .thenComparingLong(SimilarityEdgeRow::documentIdLow)
            .thenComparingLong(SimilarityEdgeRow::documentIdHigh))
        .toList()) {
      edges.add(new KnowledgeGraphEdgeDto(
          "similar-" + edge.documentIdLow() + "-" + edge.documentIdHigh(),
          edge.documentIdLow(), edge.documentIdHigh(), edge.score(),
          new KnowledgeGraphCitationDto(edge.chunkIdLow(), edge.blockIdLow(), edge.textLow()),
          new KnowledgeGraphCitationDto(edge.chunkIdHigh(), edge.blockIdHigh(), edge.textHigh())));
    }
    return new KnowledgeGraphDto(
        status,
        identity == null ? null : EmbeddingIdentityDto.from(identity),
        List.copyOf(nodes),
        List.copyOf(edges));
  }

  private EmbeddingIdentity activeIdentity() {
    EmbeddingProvider provider = embeddingProvider.orElseThrow();
    return new EmbeddingIdentity(provider.provider(), provider.model(), provider.dimensions());
  }

  private double normalizeMinimumScore(Double requestedMinimumScore) {
    if (requestedMinimumScore == null) {
      return rebuildMinimumScore;
    }
    if (!Double.isFinite(requestedMinimumScore) || requestedMinimumScore < -1.0 || requestedMinimumScore > 1.0) {
      throw new IllegalArgumentException("minimumScore must be a finite value from -1.0 to 1.0");
    }
    return requestedMinimumScore;
  }

  private static String display(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }
}
