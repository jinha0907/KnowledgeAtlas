# ExecPlan: Phase 6 - Decision Extraction and Evidence Review

## 1) Purpose / User-visible outcome
Allow a synced meeting document to produce reviewable proposed decisions with block-level evidence, without automatically accepting an LLM suggestion.

## 2) Scope / Non-goals
Scope:
- Add a document extraction fingerprint so unchanged source evidence is processed once.
- Store a concise discussion summary with each proposed decision.
- Send normalized document blocks to an optional OpenAI-compatible extractor and require structured JSON candidates.
- Persist each candidate with source-document/block evidence and expose a manual extraction endpoint.

Non-goals:
- Automatic status acceptance, decision merging across documents, graph UI, and multi-user review assignments.

## 3) Data model and idempotency
- Add `decision.discussion` and an optional `decision.extraction_run_id`.
- Add `decision_extraction_run` with a unique `(document_id, source_checksum)` fingerprint.
- A rerun with the same document checksum returns the run's existing candidates; a changed source document creates a new run and proposed candidates.

## 4) API contracts
- `POST /api/documents/{documentId}/decisions/extract` returns `disabled` without a configured provider, `existing` for an unchanged extraction, or proposed decision candidates with evidence.
- Extracted decisions always begin at `proposed`; only the existing lifecycle endpoint can accept or obsolete them.

## 5) Validation
- Unit test structured candidate validation and checksum idempotency.
- Integration test the migration, duplicate guard, and evidence persistence.
- Verify no extraction request is sent while the provider is disabled.

## 6) Risks and rollback
- Meeting-note text leaves the local machine only when the explicit extractor provider is enabled.
- Structured model output can still be low confidence; persist it as proposed and retain the exact evidence quote for review.
- Roll back by setting the provider to `none`; existing decisions and evidence remain readable.

## Decision Log
- 2026-07-17: Keep the extractor optional and reuse `OPENAI_API_KEY`; default provider remains disabled.
- 2026-07-17: Default to `gpt-4.1-mini`, which supports Chat Completions and structured outputs, while keeping the model configurable.
