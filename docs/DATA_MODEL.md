# Data Model (MVP)

## Core tables (implemented in `V2__create_mvp_core_tables.sql`)
- `source_document`
  - `id` (pk), `source_type`, `source_id`, `title`, `last_synced_at`, `raw_json(jsonb)`, `checksum`
  - unique: `(source_type, source_id)` for idempotent sync upsert
- `content_block`
  - `id` (pk), `document_id` (fk), `block_id`, `text`, `path`, `updated_at`
  - unique: `(document_id, block_id)` for stable Notion block updates
- `chunk`
  - `id` (pk), `document_id` (fk), `block_id`, `chunk_index`, `text`, `token_count`, `checksum`
  - `search_vector` generated column for FTS (`to_tsvector('simple', text)`)
  - unique: `(document_id, block_id, chunk_index)` for deterministic chunk replacement
- `embedding`
  - `chunk_id` (pk/fk), `embedding(vector(1536))`, `model`, `created_at`
- `decision`
  - `id` (pk), `title`, `status(proposed/accepted/obsolete)`, `outcome`, `supersedes_decision_id`, timestamps
- `decision_evidence`
  - `id` (pk), `decision_id` (fk), `document_id` (fk), `block_id`, `quote`, `rationale`
- `sync_job_run` (optional operational table)
  - `id`, `source_type`, `started_at`, `finished_at`, `status(running/success/failed)`, `synced_documents`, `source_watermark_at`, `error_message`
  - at most one `running` row per `source_type` (partial unique index)

## Sync invariants
- Notion source identity is `(source_type, source_id)` and must be stable across re-sync.
- Sync uses checksum to detect unchanged snapshots and skip unnecessary block/chunk rewrites.
- Chunk generation is deterministic for the same normalized input text.

## Retrieval indexes (Phase 3)
- `idx_chunk_search_vector` (GIN on `chunk.search_vector`) for keyword/FTS retrieval.
- `idx_embedding_vector_ivfflat` (ivfflat on `embedding.embedding`) for vector retrieval path.

## Decision lifecycle invariant
- Valid transitions: `proposed -> accepted -> obsolete`.
- `supersedes_decision_id` is used when a newer accepted decision supersedes an older one.

## Sync runner behavior (Phase 4)
- `sync_job_run` is now actively used by the Notion manual run endpoint.
- Incremental cutoff is the latest successful `source_watermark_at` for `source_type='notion'`; the runner starts two minutes before it to cover edits made during a prior run.
- Each run writes `running -> success|failed` status transitions with timestamps. Failed or truncated runs do not advance the watermark.
- `raw_json` stores the page and its fetched block snapshot. A changed snapshot reconciles removed source blocks by deleting their local blocks, chunks, and cascaded embeddings.
