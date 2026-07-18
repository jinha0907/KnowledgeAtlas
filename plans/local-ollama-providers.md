# Local Ollama Providers and Safe Embedding Migration

## Purpose / User-visible outcome
Run embeddings, document analysis, and decision extraction against a local Ollama server without an API key. Keep OpenAI providers available for a later, explicit retrieval-quality comparison.

## Scope / Non-goals
- Add `ollama` providers for embeddings, document analysis, and decision extraction.
- Replace the OpenAI-specific embedding dimension assumption with configured provider metadata.
- Detect incompatible persisted embeddings before backfill or hybrid search.
- Add an explicit, confirmed re-index endpoint that removes existing embeddings and regenerates them with the active provider.
- Do not install Ollama, download models, or run destructive re-indexing as part of this change.

## Data model & migrations
- Change `embedding.embedding` from `vector(1536)` to dimension-flexible `vector`.
- Store `provider` and `dimensions` alongside every embedding. Existing rows are recorded as `openai` and `1536`.
- Replace the fixed 1536 ivfflat index with a partial 1536 index for existing OpenAI rows. Other dimensions remain correct but use sequential vector retrieval until a dedicated index is added after corpus-size evaluation.

## API contracts
- `POST /api/embeddings/backfill`: returns `409 Conflict` when persisted vectors use a different provider/model/dimension.
- `POST /api/embeddings/reindex` with `{ "confirm": true }`: deletes all persisted embeddings, then backfills them with the configured provider. Missing confirmation is rejected.
- Search falls back to PostgreSQL FTS if embedding generation or compatibility validation fails.

## Step-by-step implementation plan
1. Extend the embedding provider contract with provider ID and dimensions; update OpenAI implementation and tests.
2. Implement Ollama native `/api/embed` provider and native `/api/chat` JSON providers for analysis and decision extraction.
3. Add Flyway migration and repository metadata checks; make search filter vectors by matching configuration.
4. Add the confirmed re-index API and validation tests for mismatched dimensions/models.
5. Update configuration examples and architecture/data-model/security/E2E documents with local setup, rollback, and OpenAI comparison procedure.
6. Run API tests, web lint/build, then commit the isolated feature and update issue #11.

## Risks & rollback
- A local model may return a dimension different from its configured value. The provider rejects it before persistence.
- Switching models without re-indexing could make similarity distances invalid. Compatibility checks block hybrid search/backfill; FTS remains available.
- Re-indexing intentionally deletes vectors but never source documents, blocks, chunks, decisions, or analysis records. Roll back by restoring the prior provider settings and running a confirmed re-index again.

## Decision Log
- 2026-07-18: Use Ollama native `/api/embed` and `/api/chat` endpoints rather than impersonating OpenAI. This avoids API-key configuration and makes local operation explicit.
- 2026-07-18: Store provider/model/dimension identity with every vector. A mismatch blocks backfill and degrades search to FTS; the operator must explicitly call confirmed re-indexing to change vector spaces.
- 2026-07-18: Keep the existing 1536-dimensional ivfflat path as a partial index. Local dimensions are correctness-first until corpus size justifies a separate measured index.

## Verification Record
- 2026-07-18: `cd apps/api && mvn test` passed 29 tests; the Testcontainers integration test was skipped because Docker Desktop was unavailable in this environment.
- 2026-07-18: `cd apps/web && npm run lint && npm run build` passed.
- 2026-07-18: No Ollama server or local PostgreSQL listener was available, so no external/local model call or Flyway runtime migration was executed.
