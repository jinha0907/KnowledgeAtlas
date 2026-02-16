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
  - unique: `(document_id, block_id, chunk_index)` for deterministic chunk replacement
- `embedding`
  - `chunk_id` (pk/fk), `embedding(vector(1536))`, `model`, `created_at`
- `decision`
  - `id` (pk), `title`, `status(proposed/accepted/obsolete)`, `outcome`, `supersedes_decision_id`, timestamps
- `decision_evidence`
  - `id` (pk), `decision_id` (fk), `document_id` (fk), `block_id`, `quote`, `rationale`
- `sync_job_run` (optional operational table)
  - `id`, `source_type`, `started_at`, `finished_at`, `status(running/success/failed)`, `synced_documents`, `error_message`

## Sync invariants
- Notion source identity is `(source_type, source_id)` and must be stable across re-sync.
- Sync uses checksum to detect unchanged snapshots and skip unnecessary block/chunk rewrites.
- Chunk generation is deterministic for the same normalized input text.
