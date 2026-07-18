# Project Knowledge Graph (MVP)

## Structure
- `apps/web`: Next.js frontend
- `apps/api`: Spring Boot backend
- `infra`: local infrastructure (Postgres + pgvector)
- `docs`: architecture, data model, security, plans

## Quick start
1. Prepare env:
   - `make setup`
2. Start DB:
   - `make dev-db`
3. Run API:
   - `make dev-api`
4. Run Web:
   - `make dev-web`

Web: `http://localhost:3000`
API health: `http://localhost:8080/api/health`
Swagger UI: `http://localhost:8080/swagger-ui/index.html`
OpenAPI JSON: `http://localhost:8080/v3/api-docs`

The web atlas shows synced documents, status-filtered decisions, and the stored source blocks behind each evidence quote. The local web server is allowed by default; change `CORS_ALLOWED_ORIGIN` when the web origin changes.

## Phase 3 APIs
- Search:
  - `POST /api/search`
- Decisions:
  - `POST /api/decisions`
  - `GET /api/decisions`
  - `GET /api/decisions/{id}`
  - `PATCH /api/decisions/{id}/status`
  - `POST /api/decisions/{id}/evidence`

## Phase 4 API (Notion real sync)
- `POST /api/notion/sync/run`
  - Optional body:
    - `{ "pageSize": 20, "maxPages": 20 }`

Example:
```bash
curl -s -X POST http://localhost:8080/api/notion/sync/run \
  -H "Content-Type: application/json" \
  -d '{"pageSize":20,"maxPages":20}'
```

Required env for real Notion sync:
- `NOTION_TOKEN`
- `NOTION_VERSION` (default: `2022-06-28`)

## Project and Decision Atlas
- `GET /api/documents` lists the locally synced source documents.
- `GET /api/documents/{id}` returns a document and its stored blocks for evidence review.
- `apps/web` provides the local project/decision map and buttons for sync, optional embedding backfill, and optional decision extraction.

## Embeddings and Hybrid Search
By default, the API uses keyword retrieval only. To enable OpenAI embeddings and pgvector hybrid retrieval, set the following local `.env` values:

```bash
EMBEDDING_PROVIDER=openai
OPENAI_API_KEY=your_api_key
OPENAI_EMBEDDING_MODEL=text-embedding-3-small
```

Then generate embeddings for documents synced before the provider was enabled:

```bash
curl -s -X POST http://localhost:8080/api/embeddings/backfill
```

`OPENAI_API_KEY` is never committed. New or changed chunks are backfilled after sync; unchanged chunk text retains its existing embedding.

## Decision Extraction
Decision extraction is disabled by default. Enable it only when meeting-note content may be sent to the configured OpenAI API:

```bash
DECISION_EXTRACTION_PROVIDER=openai
OPENAI_DECISION_MODEL=gpt-4.1-mini
```

Run extraction for a synced document ID:

```bash
curl -s -X POST http://localhost:8080/api/documents/1/decisions/extract
```

Candidates are stored as `proposed` with exact block quotes. Repeating extraction for unchanged source content returns the existing candidates instead of creating duplicates.

## Document Analysis
Document analysis is disabled by default. It creates a concise stored summary and up to eight reviewable tags from a synced document's blocks:

```bash
DOCUMENT_ANALYSIS_PROVIDER=openai
OPENAI_DOCUMENT_ANALYSIS_MODEL=gpt-4.1-mini
```

```bash
curl -s -X POST http://localhost:8080/api/documents/1/analysis/run
```

The same document checksum returns its prior successful analysis. Invalid provider output is rejected rather than stored, and the Atlas shows successful, pending, failed, and unavailable states.

## Evidence Graph
- `GET /api/project-graph` returns the Atlas graph read model.
- Each link is derived only from stored `decision_evidence`: `decision -> evidence block -> document`.
- The Atlas status filter retains each selected decision's complete evidence path; selecting evidence opens the local source document.

## Atlas Search
The Atlas includes a citation-first search panel backed by `POST /api/search`. It works with keyword retrieval by default and uses hybrid retrieval automatically when embeddings are configured. Selecting a result opens and highlights the cited local document block.

## Contribution
- Commit message convention: `docs/COMMIT_CONVENTION.md`

## Validation
- API unit and integration tests: `cd apps/api && mvn test`
  - The pgvector integration test uses Testcontainers. It is skipped when Docker is unavailable and runs in GitHub Actions.
- Web checks: `cd apps/web && npm ci && npm run lint && npm run build`
- GitHub Actions runs the API suite with Java 21 and the web checks with Node 22 on pushes and pull requests.
