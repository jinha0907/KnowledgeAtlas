# ExecPlan: Phase 3 - Retrieval + Decision Foundation

## 1) Purpose / User-visible outcome
Add first usable retrieval and decision tracking APIs:
- Search endpoint returning deterministic evidence citations.
- Decision endpoints supporting lifecycle transitions and evidence linking.

## 2) Scope / Non-goals
Scope:
- Retrieval API with keyword/FTS ranking + deterministic ordering.
- Decision CRUD-lite (create/list/get) + status transition + evidence attach.
- Migration/indexes for retrieval performance.

Non-goals:
- LLM answer generation.
- Real embedding query execution.
- Full meeting transcript parser.

## 3) Data model & migrations
- Add FTS search vector/index on `chunk.text`.
- Add embedding vector index placeholder for pgvector path.
- Use existing `decision` and `decision_evidence` schema with service-level transition rules.

## 4) API contracts
- `POST /api/search`
  - request: `{ "query": "...", "topK": 5 }`
  - response: `{ "answer": "...", "citations": [{score, documentId, blockId, title, text}] }`
- `POST /api/decisions`
- `GET /api/decisions`
- `GET /api/decisions/{id}`
- `PATCH /api/decisions/{id}/status`
- `POST /api/decisions/{id}/evidence`

## 5) Step-by-step implementation plan
1. Add migration for retrieval indexes.
2. Implement retrieval Controller/Service/Repository + DTOs.
3. Implement decision Controller/Service/Repository + DTOs.
4. Add service tests for retrieval no-evidence guardrail and decision transition rules.
5. Update docs (`DATA_MODEL`, `ARCHITECTURE`) for phase-3 behavior.

Validation:
- Search returns deterministic ordering and citation fields.
- Invalid decision transitions are rejected.
- Evidence rows are attachable to decisions.

## 6) Risks & rollback
Risks:
- FTS ranking behavior may need tuning for Korean/English mixed text.
- Migration/index syntax can vary by Postgres version/settings.

Rollback:
- Revert phase-3 commit and database to pre-V3 migration state.

## Decision Log
- 2026-02-17: Chose deterministic SQL ordering (`score DESC, chunk_id ASC`) for stable API responses.
- 2026-02-17: Enforced decision status transitions in service layer to keep DB schema simple in MVP.
- 2026-02-17: Switched search repository test seam to interface-based mocking for Java 25 compatibility (avoid Mockito inline class mock instrumentation issue).
