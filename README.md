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

## Phase 3 APIs
- Search:
  - `POST /api/search`
- Decisions:
  - `POST /api/decisions`
  - `GET /api/decisions`
  - `GET /api/decisions/{id}`
  - `PATCH /api/decisions/{id}/status`
  - `POST /api/decisions/{id}/evidence`
