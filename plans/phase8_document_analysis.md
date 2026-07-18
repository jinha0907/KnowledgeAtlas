# Phase 8: Reviewable Document Analysis

## Goal
Complete the remaining MVP classification capability by deriving a concise document summary and bounded tags from locally stored Notion blocks, without treating generated output as source truth.

## Design
- `document_analysis_run` persists each document checksum's analysis state and result. Its unique `(document_id, source_checksum)` key makes completed analyses idempotent.
- The optional provider receives only a document title and stored blocks. It is absent unless `DOCUMENT_ANALYSIS_PROVIDER=openai` is configured.
- Provider output is accepted only when it has a nonblank bounded summary and one to eight normalized tags. Invalid output fails the run instead of becoming visible data.
- Document list/detail read models include the latest run. The Atlas displays successful summaries/tags, running state, failed state, or an unavailable prompt.

## Validation
- Unit tests cover disabled behavior and output normalization/validation.
- The PostgreSQL integration test proves migration, persisted result reads, and checksum uniqueness.
- Run API tests plus web lint/build before committing.

## Non-goals
- Writing classifications back to Notion, automatic taxonomy curation, and treating a generated tag as an authorization or decision signal.
