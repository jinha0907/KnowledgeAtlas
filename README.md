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

## Contribution
- Commit message convention: `docs/COMMIT_CONVENTION.md`
