# Phase 10: Citation-First Search UI

## Goal
Make the existing retrieval API usable inside the Atlas while preserving the source evidence returned by the backend.

## Design
- The browser submits nonempty queries to `POST /api/search` with a bounded `topK` of six.
- Results display exactly the backend citation's title, block ID, text, and score. The UI generates no answer or citation.
- Selecting a result opens the existing local document inspector and highlights its cited block, including when that block would not normally be among the first previewed blocks.
- The API's configured hybrid retrieval and FTS fallback remain backend behavior; the UI does not need provider-specific branching.

## Validation
- Run web lint/build plus API tests and GitHub Actions.

## Non-goals
- Chat answer generation, query persistence, reranking, and external search providers.
