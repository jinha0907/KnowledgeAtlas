---
name: notion-ingest
description: Use this when building Notion sync: incremental fetch, normalization, chunking, checksum/dedup, and upserting into Postgres tables and pgvector.
---

## Rules
- Treat Notion data as source-of-truth; store a normalized snapshot in source_document.raw_json.
- Prefer incremental sync (based on last_synced_at / updated time) with retry + backoff.
- Persist stable identifiers: source_id, block_id.
- Generate chunk records deterministically; update embeddings only when chunk text checksum changes.

## Outputs
- Sync job that can run repeatedly without duplicating data.
- Logs/metrics-friendly job status rows (optional table sync_job_run).
