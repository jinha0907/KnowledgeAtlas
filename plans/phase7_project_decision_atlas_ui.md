# Phase 7: Project and Decision Atlas UI

## Goal
Provide a local, responsive browser surface for inspecting synced Notion documents, reviewing decision candidates, and following each decision to its stored evidence.

## Scope
- Add read-only document list and document-with-blocks API endpoints.
- Permit the local Next.js origin to call `/api/**` through an explicit CORS setting.
- Build the project map / decision map screen in `apps/web`, with sync, embedding-backfill, and decision-extraction actions.
- Keep decision evidence reviewable by opening the local source document and its saved blocks.

## Validation
- Extend the pgvector integration test to cover document list/detail reads after sync.
- Run `cd apps/api && mvn test` and `cd apps/web && npm run lint && npm run build`.

## Non-goals
- Authentication, multi-workspace authorization, graph-layout persistence, and decision approval workflows are outside this phase.
