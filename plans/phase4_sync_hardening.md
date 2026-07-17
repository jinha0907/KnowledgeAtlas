# ExecPlan: Phase 4 - Notion Sync Correctness Hardening

## 1) Purpose / User-visible outcome
Make Notion sync safe to rerun without missing concurrent edits, retaining stale deleted blocks, or losing failure job records.

## 2) Scope / Non-goals
Scope:
- Durable `sync_job_run` state transitions and a source-update watermark.
- Overlap-window incremental fetch.
- Recursive block traversal with stable hierarchical paths.
- Reconciliation of blocks removed from the source snapshot.
- Tests for critical runner and reconciliation behavior.

Non-goals:
- OAuth, multitenancy, scheduler, embeddings, and LLM extraction.

## 3) Data model & migrations
- Add `source_watermark_at` to `sync_job_run`.
- Add a partial unique index to prevent concurrent `running` jobs for one source type.
- Reuse existing foreign keys: deleting chunks removes embeddings through `embedding.chunk_id` cascade.

## 4) API contracts
- `POST /api/notion/sync/run` remains unchanged.
- A concurrent run returns a conflict instead of performing duplicate work.
- Successful responses continue to return document/block/chunk counts; `since` represents the safe overlap start.

## 5) Step-by-step implementation plan
1. Add migration and repository operations for source watermark and active-run guard.
2. Remove the long outer transaction from the runner; persist job lifecycle in independent operations.
3. Use the maximum observed Notion source update time and a small overlap window for the next run.
4. Recursively fetch nested blocks, retaining deterministic paths.
5. Reconcile source-missing blocks and their chunks after a changed document sync.
6. Add tests and update architecture/data-model docs.

Validation:
- A failed run persists `status=failed`.
- Repeated and overlapping runs do not duplicate rows.
- Deleting a source block removes its block/chunks locally.
- Nested Notion blocks are included in the normalized sync payload.
- A page-limit truncation fails without advancing the watermark, rather than silently skipping older results.

## 6) Risks & rollback
Risks:
- Overlap repeats recent pages and adds API work.
- Recursive traversal can amplify calls on deeply nested content.

Rollback:
- Revert the application commit and restore a DB snapshot before the new migration if required.

## Decision Log
- 2026-07-17: Use source-update watermark plus a bounded overlap instead of `finished_at` as the incremental cutoff.
- 2026-07-17: Keep one manual active run per source type through a partial unique index; scheduler work remains out of scope.
- 2026-07-17: Treat a `maxPages` truncation as a failed run so an incomplete Notion search cannot advance the incremental watermark.
