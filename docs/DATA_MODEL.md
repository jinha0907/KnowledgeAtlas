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
  - `id` (pk), `title`, `status(proposed/accepted/obsolete)`, `discussion`, `outcome`, optional `confidence`, `supersedes_decision_id`, optional `extraction_run_id`, timestamps
- `decision_evidence`
  - `id` (pk), `decision_id` (fk), `document_id` (fk), `block_id`, `quote`, `rationale`
- `decision_extraction_run`
  - `id`, `document_id` (fk), `source_checksum`, `status(running/success/failed)`, `extracted_decisions`, `error_message`, timestamps
  - unique: `(document_id, source_checksum)` prevents duplicate extraction for unchanged source evidence
- `document_analysis_run`
  - `id`, `document_id` (fk), `source_checksum`, `status(running/success/failed)`, `summary`, `tags(text[])`, `error_message`, timestamps
  - unique: `(document_id, source_checksum)` prevents duplicate summary/tag analyses for unchanged source evidence
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

## Embedding invariants (Phase 5)
- The current schema stores 1536-dimensional vectors, matching the configured `text-embedding-3-small` default.
- Replacing a chunk deletes its old embedding through the foreign-key cascade. Unchanged chunk checksums retain their chunk ID and existing embedding.
- Only chunks without an embedding are sent to the configured provider. `POST /api/embeddings/backfill` processes existing missing rows after a provider is enabled.

## Decision lifecycle invariant
- Valid transitions: `proposed -> accepted -> obsolete`.
- `supersedes_decision_id` is used when a newer accepted decision supersedes an older one.
- Extracted candidates are always created as `proposed`. Their evidence quote must be copied from the referenced stored block.

## Evidence graph invariant (Phase 9)
- The graph is a read model only; it creates no graph table or inferred relation.
- Every graph path is exactly `decision -> decision_evidence -> source_document/block`. Node IDs are derived from persisted primary keys and edge ordering is deterministic by evidence ID.

## Document analysis invariant (Phase 8)
- A successful analysis contains a nonblank summary of at most 800 characters and one to eight normalized tags of at most 80 characters each.
- Analysis is derived from the stored document snapshot, never written back to Notion, and is reviewable metadata rather than source truth.
- Failed runs may be retried for the same checksum; successful runs are returned idempotently. Read APIs show only the run matching the document's current checksum.

## Sync runner behavior (Phase 4)
- `sync_job_run` is now actively used by the Notion manual run endpoint.
- Incremental cutoff is the latest successful `source_watermark_at` for `source_type='notion'`; the runner starts two minutes before it to cover edits made during a prior run.
- Each run writes `running -> success|failed` status transitions with timestamps. Failed or truncated runs do not advance the watermark.
- `raw_json` stores the page and its fetched block snapshot. A changed snapshot reconciles removed source blocks by deleting their local blocks, chunks, and cascaded embeddings.
