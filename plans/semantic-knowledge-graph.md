# Semantic Knowledge Graph

## Purpose / User-visible outcome
Add an Obsidian-style interactive graph for the synced Notion knowledge base. Each node is a local source document. A visible edge means the two documents are semantically related under the active embedding model, and users can inspect the pair of stored chunks that explains that relation. The existing Decision Graph remains an evidence-only graph and is not weakened or merged with inferred similarity.

## Scope / Non-goals
- Build a sparse, persisted document-similarity read model from existing chunk embeddings.
- Provide explicit rebuild, graph-read, and current-state APIs.
- Add an interactive browser graph with pan, zoom, drag, node selection, edge inspection, and a score threshold control.
- Reuse existing local/OpenAI embedding identities. The graph never calls an LLM.
- Do not infer decisions, create decision evidence, ingest attachment/PDF contents, or attempt a dense all-pairs graph on every page load.
- Whole-document, windowed decision extraction is a follow-up phase.

## Data model & migrations
Create `document_similarity` as a sparse derived table:
- Canonical pair: `document_id_low < document_id_high`, each referencing `source_document`.
- `score` is cosine similarity between the two documents' persisted chunk-embedding centroids.
- `source_chunk_id` and `target_chunk_id` retain the explanation evidence.
- `provider`, `model`, and `dimensions` identify the vector space used to build the edge.
- `source_checksum_low` and `source_checksum_high` bind the edge to document snapshots.
- A unique document pair prevents duplicate edges; indexes support graph reads.

The rebuild deletes only this derived table and repopulates it from chunks that have the configured active embedding identity. It scores document-centroid pairs, finds a bounded representative chunk pair near each document centroid, then keeps an edge only when it is in the top four neighbours of at least one endpoint and meets the configured minimum score. This prevents a hairball while retaining explainable local links.

## API contracts
- `POST /api/knowledge-graph/rebuild`
  - Recomputes the derived similarity graph from current embeddings.
  - Response: `{ "status": "success", "documents": 4, "edges": 5 }`.
  - Returns `disabled` when no embedding provider is configured and rejects an identity mismatch.
- `GET /api/knowledge-graph?minimumScore=0.55`
  - Returns document nodes plus only persisted edges at or above the requested score.
  - Nodes include document identity, title, summary/tags when available, and block count.
  - Edges include score and two exact local chunk citations.

## Step-by-step implementation plan
1. Add the migration, repository contract, and JDBC implementation for similarity rows and current-identity candidate queries.
2. Add graph DTOs, a service that constructs sparse explainable edges, and controller endpoints. Keep the existing project graph untouched.
3. Add service/repository tests for identity isolation, self-edge exclusion, pair deduplication, sparse-neighbour filtering, and citation preservation.
4. Add a Knowledge Graph mode to the Atlas. It fetches the read API, supports SVG pan/zoom, node drag, score threshold filtering, and displays selected document/edge evidence in the existing inspector.
5. Update architecture/data model documentation and run API tests plus web lint/build.
6. Rebuild the local graph against the current Ollama embeddings, verify graph data and browser behavior, then commit/push and close Issue #15.

## Risks & rollback
- Similarity can be noisy for a small or heterogeneous corpus. The graph labels edges as `related`, exposes source chunks, and lets the user raise the threshold; it never claims a decision or causal relation.
- A model change invalidates the current vector space. The service only reads rows matching the active identity, and rebuild replaces only `document_similarity` rows.
- Pairwise comparisons grow with corpus size. The first version limits the derived graph to sparse neighbours and runs only on explicit rebuild; future work can add incremental affected-document rebuilds or approximate nearest-neighbour candidate selection.
- Rollback consists of deleting the derived rows or reverting the migration. Source documents, chunks, embeddings, and decision records are not modified.
