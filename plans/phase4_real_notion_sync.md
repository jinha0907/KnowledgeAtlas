# ExecPlan: Phase 4 - Real Notion Incremental Sync

## 1) Purpose / User-visible outcome
Enable real data collection from Notion into local DB with idempotent incremental sync.

## 2) Scope / Non-goals
Scope:
- Notion API client (search + block children fetch)
- Manual sync-run endpoint
- Incremental window based on the latest successful source-update watermark
- Retry/backoff and sync_job_run status tracking

Non-goals:
- OAuth multi-tenant auth
- Automated scheduler (manual trigger first)

## 3) Data model & migrations
- Reuse existing content tables from V2.
- Add V4 to store `sync_job_run.source_watermark_at` and prevent concurrent running jobs for the same source type.

## 4) API contracts
- `POST /api/notion/sync/run`
  - request: `{ "pageSize": 20, "maxPages": 50 }` (optional)
  - response: `{ "status", "jobRunId", "since", "syncedDocuments", "changedDocuments", "upsertedBlocks", "upsertedChunks" }`

## 5) Step-by-step implementation plan
1. Add Notion client interface + HTTP implementation.
2. Add sync job run repository for start/success/failure + latest successful source watermark lookup.
3. Add sync runner service to fetch changed pages and invoke existing `NotionSyncService`.
4. Add controller endpoint for manual sync trigger.
5. Add tests for successful watermarking, bounded overlap, failures, and page-limit truncation.
6. Update docs and env examples.

## 6) Risks & rollback
Risks:
- Notion API rate limits and transient failures.
- A deeply nested workspace can produce many block API calls; traversal is capped at depth 32.
- A run that reaches `maxPages` must be retried with a larger limit; it intentionally does not advance the watermark.

Rollback:
- Revert phase-4 commit; schema remains backward compatible.

## Decision Log
- 2026-02-17: Manual trigger endpoint first, scheduler later after connector stability.
- 2026-07-17: Use `source_watermark_at` with a two-minute overlap, rather than `finished_at`, to avoid missing edits made during a run.
- 2026-07-17: Recursively collect nested blocks and reconcile source-deleted blocks on changed snapshots.
