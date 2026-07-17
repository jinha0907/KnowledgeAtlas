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

## Contribution
- Commit message convention: `docs/COMMIT_CONVENTION.md`

## Validation
- API unit and integration tests: `cd apps/api && mvn test`
  - The pgvector integration test uses Testcontainers. It is skipped when Docker is unavailable and runs in GitHub Actions.
- Web checks: `cd apps/web && npm ci && npm run lint && npm run build`
- GitHub Actions runs the API suite with Java 21 and the web checks with Node 22 on pushes and pull requests.
