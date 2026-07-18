# Provider Status and Retrieval Evaluation

## Purpose / User-visible outcome
Expose embedding readiness in the Atlas before users run semantic search or re-index. Provide a source-free evaluation template for comparing local Ollama and separately approved OpenAI retrieval runs.

## Scope / Non-goals
- Add a read-only embedding status endpoint and Atlas status card.
- Require an explicit UI checkbox before the existing destructive re-index API is called.
- Add a versioned evaluation template and result-recording procedure using document/block IDs and ranks only.
- Do not call providers from status loading, persist benchmark results, or install/download models.

## Data model & migrations
No schema change. Status is derived from `chunk` and `embedding` rows plus active provider configuration.

## API contracts
- `GET /api/embeddings/status` returns active identity when configured, persisted identities, embedding/chunk counts, and `reindexRequired`.

## Implementation plan
1. Add repository count queries and service-level status derivation without inference.
2. Add status DTO/controller endpoint and service tests for disabled, incomplete, ready, and mismatched states.
3. Load status in Atlas; display active/persisted state and gate re-indexing behind a checkbox.
4. Add a source-free evaluation template and documented local/OpenAI comparison procedure.
5. Run API tests and web lint/build; commit and record issue status.

## Risks & rollback
- Status is advisory and may change between read and re-index; the backend remains the enforcement point.
- The UI confirmation is not relied on for safety because the backend also requires `confirm=true`.
