# PRD (MVP Draft)

## Product
Project Knowledge Graph + Decision Tracker

## Target outcome
- Sync copied Notion documents to Postgres.
- Build searchable knowledge with pgvector.
- Extract meeting decisions and connect them to evidence.

## MVP scope
- Single workspace, single-user operation.
- Notion sync pipeline (incremental).
- Retrieval API with citations.
- Basic web visualization (project/decision map).

## Success criteria
- Sync from at least one Notion test workspace.
- Search returns topK with document/block evidence.
- Decision records are queryable with supporting evidence.
- Automated decision candidates remain reviewable proposals; they are never accepted automatically.
