# ExecPlan: Phase 2 - Ingest Foundation

## 1) Purpose / User-visible outcome
Introduce the first production-shaped backend foundation for ingestion:
- Core schema for documents/blocks/chunks/embeddings/decisions.
- Notion sync API skeleton with idempotent upsert behavior.
- Deterministic chunk generation.

Outcome: developers can POST normalized Notion-like payloads and persist data without duplicates.

## 2) Scope / Non-goals
Scope:
- Flyway migration for core tables + indexes.
- Controller/Service/Repository layering for ingestion path.
- DTO-based API boundary.
- Basic unit tests for chunking.

Non-goals:
- Real Notion API client and scheduler.
- Real embedding model call.
- Retrieval API/decision extraction implementation.

## 3) Data model & migrations
- Add `V2__create_mvp_core_tables.sql`:
  - `source_document`, `content_block`, `chunk`, `embedding`, `decision`, `decision_evidence`, `sync_job_run`.
- Add unique keys for idempotent upserts (`source_type + source_id`, `document_id + block_id`, `document_id + block_id + chunk_index`).

## 4) API contracts
- `POST /api/notion/sync`
  - Request: source metadata + raw_json + normalized blocks.
  - Response: document id + upserted block/chunk counts + status.

## 5) Step-by-step implementation plan
1. Add migration SQL for core schema and indexes.
2. Add DTOs + `NotionSyncController` endpoint.
3. Add `NotionSyncService` to orchestrate upserts and deterministic chunking.
4. Add repositories for document/block/chunk persistence with upsert SQL.
5. Add unit tests for chunk determinism and splitting behavior.
6. Update `docs/DATA_MODEL.md` to reflect concrete columns and constraints.

Validation:
- App starts with migration applied.
- Repeated sync with same payload does not duplicate blocks/chunks.
- Unit test passes for chunker.

## 6) Risks & rollback
Risks:
- SQL portability around upsert semantics.
- Chunk policy changes can cause re-index churn.

Rollback:
- Revert phase-2 commit and roll DB to pre-V2 snapshot in local/dev.

## Decision Log
- 2026-02-16: Chose checksum-based short-circuit at document level to avoid unnecessary re-upsert of blocks/chunks.
- 2026-02-16: Chose deterministic char-based chunking (fixed size + overlap) for MVP simplicity and repeatability.
