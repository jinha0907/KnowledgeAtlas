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
  - `chunk_id` (pk/fk), `embedding(vector)`, `provider`, `model`, `dimensions`, `created_at`
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
- `document_similarity`
  - canonical derived pair: `document_id_low < document_id_high`, similarity `score`, representative `chunk_id_low/high`, active embedding identity, source checksums, and `created_at`
  - foreign keys cascade when a source document or representative chunk is replaced; this derived table never changes source content or decisions

## Sync invariants
- Notion source identity is `(source_type, source_id)` and must be stable across re-sync.
- Sync checksum is calculated from source identity, normalized title, and sorted block IDs/text/paths. Stored `raw_json` and block update timestamps are diagnostic metadata, not checksum input, so volatile Notion API fields cannot cause rewrites.
- An unchanged checksum skips all `source_document`, block, and chunk writes; `raw_json` remains the last changed source snapshot.
- Chunk generation is deterministic for the same normalized input text.

## Retrieval indexes (Phase 3)
- `idx_chunk_search_vector` (GIN on `chunk.search_vector`) for keyword/FTS retrieval.
- `idx_embedding_vector_ivfflat` (ivfflat on `embedding.embedding`) for vector retrieval path.

## Embedding invariants (Phase 5)
- Each stored vector records its `(provider, model, dimensions)` identity. Existing pre-Phase-11 rows migrate as `openai/text-embedding-3-small/1536` only when that was the configured default; test/manual rows retain their stored model with provider `openai` and dimensions `1536`.
- Replacing a chunk deletes its old embedding through the foreign-key cascade. Unchanged chunk checksums retain their chunk ID and existing embedding.
- Only chunks without an embedding are sent to the configured provider. `POST /api/embeddings/backfill` processes existing missing rows after a provider is enabled.
- Backfill is rejected when the active provider identity differs from stored rows. `POST /api/embeddings/reindex` is the only supported model-switch operation; it deletes `embedding` rows only, then regenerates them.

## Decision lifecycle invariant
- Valid transitions: `proposed -> accepted -> obsolete`.
- `supersedes_decision_id` is used when a newer accepted decision supersedes an older one.
- Extracted candidates are always created as `proposed`. Their evidence quote must be copied from the referenced stored block.

## Evidence graph invariant (Phase 9)
- The graph is a read model only; it creates no graph table or inferred relation.
- Every graph path is exactly `decision -> decision_evidence -> source_document/block`. Node IDs are derived from persisted primary keys and edge ordering is deterministic by evidence ID.

## Semantic graph invariant (Phase 14)
- `document_similarity` is a rebuildable read model, distinct from the evidence-only decision graph. It never creates a decision, decision evidence, or source link.
- Rebuild considers chunks only when `(provider, model, dimensions)` equals the configured active embedding identity. It calculates candidate scores from document embedding centroids, keeps only top neighbours over the configured threshold, then stores a bounded representative chunk pair for inspection.
- A graph read filters rows by the active identity and both current source checksums. Re-indexing or source changes therefore require an explicit rebuild before old similarity is shown again.

## Document analysis invariant (Phase 8)
- A successful analysis contains a nonblank summary of at most 800 characters and one to eight normalized tags of at most 80 characters each.
- Analysis is derived from the stored document snapshot, never written back to Notion, and is reviewable metadata rather than source truth.
- Failed runs may be retried for the same checksum; successful runs are returned idempotently. Read APIs show only the run matching the document's current checksum.

## Sync runner behavior (Phase 4)
- `sync_job_run` is now actively used by the Notion manual run endpoint.
- Incremental cutoff is the latest successful `source_watermark_at` for `source_type='notion'`; the runner starts two minutes before it to cover edits made during a prior run.
- Each run writes `running -> success|failed` status transitions with timestamps. Failed or truncated runs do not advance the watermark.
- `raw_json` stores the page and its fetched block snapshot. A changed snapshot reconciles removed source blocks by deleting their local blocks, chunks, and cascaded embeddings.
