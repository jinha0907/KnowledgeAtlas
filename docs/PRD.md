# PRD (MVP Draft)

## Product
Project Knowledge Graph + Decision Tracker

## Target outcome
- Sync copied Notion documents to Postgres.
- Build reviewable document summaries and tags from copied content.
- Build searchable knowledge with pgvector.
- Extract meeting decisions and connect them to evidence.

## MVP scope
- Single workspace, single-user operation.
- Notion sync pipeline (incremental).
- Retrieval API with citations.
- Optional document summary/tag analysis with explicit external-provider opt-in.
- Basic web visualization (semantic document map and evidence-only decision map).

## Success criteria
- Sync from at least one Notion test workspace.
- Search returns topK with document/block evidence.
- Decision records are queryable with supporting evidence.
- Automated decision candidates remain reviewable proposals; they are never accepted automatically.
- Document summaries and tags are available for the current synced snapshot and remain reviewable metadata.
- Semantic document links expose their similarity score and the two stored chunks used as representative evidence; they do not imply a decision or causality.
