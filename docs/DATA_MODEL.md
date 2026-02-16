# Data Model (MVP)

## Core tables (suggested)
- source_document
  - id, source_type(notion), source_id, title, last_synced_at, raw_json, checksum
- content_block
  - id, document_id, block_id, text, path, updated_at
- chunk
  - id, document_id, block_id, chunk_index, text, token_count
- embedding
  - chunk_id (pk/fk), embedding(vector), model, created_at
- decision
  - id, title, status(proposed/accepted/obsolete), outcome, created_at, updated_at
- decision_evidence
  - id, decision_id, document_id, block_id, quote, rationale
