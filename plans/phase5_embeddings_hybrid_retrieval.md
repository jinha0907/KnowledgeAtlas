# ExecPlan: Phase 5 - Embeddings and Hybrid Retrieval

## 1) Purpose / User-visible outcome
Enable semantic-plus-keyword search while preserving document and block evidence citations.

## 2) Scope / Non-goals
Scope:
- Configurable OpenAI embedding provider using `text-embedding-3-small`.
- Backfill only chunks without an embedding, including a manual API for existing data.
- Preserve embeddings when a chunk checksum has not changed.
- Deterministic pgvector and FTS reciprocal-rank fusion.

Non-goals:
- Generated natural-language answers, reranking models, provider failover, and multi-tenant credentials.

## 3) Data model
- Reuse `embedding.embedding vector(1536)`; no migration is required.
- Replacing changed chunk rows deletes obsolete embeddings through the existing foreign key cascade.

## 4) API contracts
- `POST /api/search` uses hybrid retrieval when a provider is configured and falls back to FTS when it is not available.
- `POST /api/embeddings/backfill` returns `success` with processed document/chunk counts or `disabled` when no provider is configured.

## 5) Validation
- Unit test: missing chunks are embedded and provider absence is a no-op.
- Unit test: configured search calls hybrid retrieval.
- Integration test: unchanged chunks keep their IDs/embeddings and changed chunks remove stale embeddings.

## 6) Risks and rollback
- External embedding calls add cost and rate-limit exposure; use the explicit provider setting and bounded batches.
- Provider failures do not roll back source ingestion; search falls back to FTS.
- Roll back by setting `EMBEDDING_PROVIDER=none`; existing embeddings remain inert but safe.

## Decision Log
- 2026-07-17: Use `text-embedding-3-small` because its default 1536 dimensions match the existing pgvector schema.
- 2026-07-17: Use reciprocal-rank fusion with a fixed rank constant (60) to combine incomparable FTS and cosine scores deterministically.
