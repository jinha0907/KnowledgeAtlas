package com.projectkg.api.knowledgegraph.repository;

import com.projectkg.api.embedding.service.EmbeddingIdentity;
import java.sql.Array;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcKnowledgeGraphRepository implements KnowledgeGraphRepository {
  private final JdbcTemplate jdbcTemplate;

  public JdbcKnowledgeGraphRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void deleteAll() {
    jdbcTemplate.update("DELETE FROM document_similarity");
  }

  @Override
  public int countDocumentsWithEmbeddings(EmbeddingIdentity identity) {
    Integer count = jdbcTemplate.queryForObject(
        """
        SELECT COUNT(DISTINCT c.document_id)
        FROM chunk c
        JOIN embedding e ON e.chunk_id = c.id
        WHERE e.provider = ? AND e.model = ? AND e.dimensions = ?
        """,
        Integer.class,
        identity.provider(), identity.model(), identity.dimensions());
    return count == null ? 0 : count;
  }

  @Override
  public List<SimilarityCandidate> findSparseCandidates(
      EmbeddingIdentity identity, double minimumScore, int maxNeighbors
  ) {
    return jdbcTemplate.query(
        """
        WITH document_vectors AS (
          SELECT c.document_id, sd.checksum, AVG(e.embedding) AS centroid
          FROM chunk c
          JOIN embedding e ON e.chunk_id = c.id
          JOIN source_document sd ON sd.id = c.document_id
          WHERE e.provider = ? AND e.model = ? AND e.dimensions = ?
          GROUP BY c.document_id, sd.checksum
        ),
        directed_pairs AS (
          SELECT source.document_id AS source_document_id,
                 target.document_id AS target_document_id,
                 source.checksum AS source_checksum,
                 target.checksum AS target_checksum,
                 1 - (source.centroid <=> target.centroid) AS score,
                 ROW_NUMBER() OVER (
                   PARTITION BY source.document_id
                   ORDER BY source.centroid <=> target.centroid ASC, target.document_id ASC
                 ) AS neighbour_rank
          FROM document_vectors source
          JOIN document_vectors target ON source.document_id <> target.document_id
        ),
        sparse_pairs AS (
          SELECT LEAST(source_document_id, target_document_id) AS document_id_low,
                 GREATEST(source_document_id, target_document_id) AS document_id_high,
                 score,
                 CASE WHEN source_document_id < target_document_id THEN source_checksum ELSE target_checksum END
                   AS source_checksum_low,
                 CASE WHEN source_document_id < target_document_id THEN target_checksum ELSE source_checksum END
                   AS source_checksum_high
          FROM directed_pairs
          WHERE neighbour_rank <= ? AND score >= ?
        )
        SELECT DISTINCT ON (document_id_low, document_id_high)
               document_id_low, document_id_high, score, source_checksum_low, source_checksum_high
        FROM sparse_pairs
        ORDER BY document_id_low, document_id_high, score DESC
        """,
        (rs, rowNum) -> new SimilarityCandidate(
            rs.getLong("document_id_low"),
            rs.getLong("document_id_high"),
            rs.getDouble("score"),
            rs.getString("source_checksum_low"),
            rs.getString("source_checksum_high")),
        identity.provider(), identity.model(), identity.dimensions(), maxNeighbors, minimumScore);
  }

  @Override
  public Optional<SimilarityEvidence> findBestEvidence(
      long documentIdLow, long documentIdHigh, EmbeddingIdentity identity, int representativeChunkLimit
  ) {
    return jdbcTemplate.query(
            """
            WITH document_vectors AS (
              SELECT c.document_id, AVG(e.embedding) AS centroid
              FROM chunk c
              JOIN embedding e ON e.chunk_id = c.id
              WHERE e.provider = ? AND e.model = ? AND e.dimensions = ?
                AND c.document_id IN (?, ?)
              GROUP BY c.document_id
            ),
            low_chunks AS (
              SELECT c.id, e.embedding
              FROM chunk c
              JOIN embedding e ON e.chunk_id = c.id
              WHERE c.document_id = ?
                AND e.provider = ? AND e.model = ? AND e.dimensions = ?
              ORDER BY e.embedding <=> (SELECT centroid FROM document_vectors WHERE document_id = ?) ASC, c.id ASC
              LIMIT ?
            ),
            high_chunks AS (
              SELECT c.id, e.embedding
              FROM chunk c
              JOIN embedding e ON e.chunk_id = c.id
              WHERE c.document_id = ?
                AND e.provider = ? AND e.model = ? AND e.dimensions = ?
              ORDER BY e.embedding <=> (SELECT centroid FROM document_vectors WHERE document_id = ?) ASC, c.id ASC
              LIMIT ?
            )
            SELECT low_chunks.id AS chunk_id_low, high_chunks.id AS chunk_id_high
            FROM low_chunks
            CROSS JOIN high_chunks
            ORDER BY low_chunks.embedding <=> high_chunks.embedding ASC,
                     low_chunks.id ASC, high_chunks.id ASC
            LIMIT 1
            """,
            (rs, rowNum) -> new SimilarityEvidence(rs.getLong("chunk_id_low"), rs.getLong("chunk_id_high")),
            identity.provider(), identity.model(), identity.dimensions(), documentIdLow, documentIdHigh,
            documentIdLow, identity.provider(), identity.model(), identity.dimensions(), documentIdLow,
            representativeChunkLimit,
            documentIdHigh, identity.provider(), identity.model(), identity.dimensions(), documentIdHigh,
            representativeChunkLimit)
        .stream()
        .findFirst();
  }

  @Override
  public void upsert(SimilarityEdge edge) {
    jdbcTemplate.update(
        """
        INSERT INTO document_similarity (
          document_id_low, document_id_high, score, chunk_id_low, chunk_id_high,
          provider, model, dimensions, source_checksum_low, source_checksum_high
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (document_id_low, document_id_high)
        DO UPDATE SET score = EXCLUDED.score, chunk_id_low = EXCLUDED.chunk_id_low,
          chunk_id_high = EXCLUDED.chunk_id_high, provider = EXCLUDED.provider,
          model = EXCLUDED.model, dimensions = EXCLUDED.dimensions,
          source_checksum_low = EXCLUDED.source_checksum_low,
          source_checksum_high = EXCLUDED.source_checksum_high, created_at = NOW()
        """,
        edge.documentIdLow(), edge.documentIdHigh(), edge.score(), edge.chunkIdLow(), edge.chunkIdHigh(),
        edge.identity().provider(), edge.identity().model(), edge.identity().dimensions(),
        edge.sourceChecksumLow(), edge.sourceChecksumHigh());
  }

  @Override
  public List<DocumentNodeRow> findDocumentNodes() {
    return jdbcTemplate.query(
        """
        SELECT sd.id, sd.title, dar.summary, dar.tags,
               (SELECT COUNT(*) FROM content_block cb WHERE cb.document_id = sd.id) AS block_count
        FROM source_document sd
        LEFT JOIN document_analysis_run dar
          ON dar.document_id = sd.id AND dar.source_checksum = sd.checksum AND dar.status = 'success'
        ORDER BY sd.id ASC
        """,
        (rs, rowNum) -> new DocumentNodeRow(
            rs.getLong("id"),
            rs.getString("title"),
            rs.getString("summary"),
            readTags(rs.getArray("tags")),
            rs.getInt("block_count")));
  }

  @Override
  public List<SimilarityEdgeRow> findEdges(EmbeddingIdentity identity, double minimumScore) {
    return jdbcTemplate.query(
        """
        SELECT ds.document_id_low, ds.document_id_high, ds.score,
               cl.id AS chunk_id_low, cl.block_id AS block_id_low, cl.text AS text_low,
               ch.id AS chunk_id_high, ch.block_id AS block_id_high, ch.text AS text_high
        FROM document_similarity ds
        JOIN source_document dl ON dl.id = ds.document_id_low AND dl.checksum = ds.source_checksum_low
        JOIN source_document dh ON dh.id = ds.document_id_high AND dh.checksum = ds.source_checksum_high
        JOIN chunk cl ON cl.id = ds.chunk_id_low
        JOIN chunk ch ON ch.id = ds.chunk_id_high
        WHERE ds.provider = ? AND ds.model = ? AND ds.dimensions = ? AND ds.score >= ?
        ORDER BY ds.score DESC, ds.document_id_low ASC, ds.document_id_high ASC
        """,
        (rs, rowNum) -> new SimilarityEdgeRow(
            rs.getLong("document_id_low"),
            rs.getLong("document_id_high"),
            rs.getDouble("score"),
            rs.getLong("chunk_id_low"),
            rs.getString("block_id_low"),
            rs.getString("text_low"),
            rs.getLong("chunk_id_high"),
            rs.getString("block_id_high"),
            rs.getString("text_high")),
        identity.provider(), identity.model(), identity.dimensions(), minimumScore);
  }

  @Override
  public boolean hasEdges() {
    Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM document_similarity", Long.class);
    return count != null && count > 0;
  }

  private List<String> readTags(Array tags) throws SQLException {
    if (tags == null || tags.getArray() == null) {
      return List.of();
    }
    Object value = tags.getArray();
    if (value instanceof String[] strings) {
      return List.of(strings);
    }
    return List.of();
  }
}
