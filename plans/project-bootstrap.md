# ExecPlan: Initial Project Bootstrap

## 1) Purpose / User-visible outcome
Set up a runnable MVP baseline with:
- `apps/web` (Next.js app)
- `apps/api` (Spring Boot API)
- PostgreSQL + pgvector via Docker Compose

User-visible outcome: one command starts infra; API runs against Postgres; web app runs and can call API health endpoint.

## 2) Scope / Non-goals
Scope:
- Monorepo directory scaffold
- Basic API health endpoint and DB connectivity config
- Basic web home page and API base URL wiring
- Local env examples and run instructions

Non-goals:
- Notion sync pipeline implementation
- Retrieval/decision business logic
- Production deployment manifests

## 3) Data model & migrations
- Add baseline migration for enabling `vector` extension.
- Add placeholder table set creation to later align with `docs/DATA_MODEL.md` (initial minimal schema only if needed).

## 4) API contracts
- `GET /api/health` -> `{ "status": "ok" }`
- `GET /api/db/health` -> `{ "status": "ok", "database": "up" }` or error status

## 5) Step-by-step implementation plan
1. Create top-level structure: `apps/web`, `apps/api`, `infra/`.
2. Add `docker-compose.yml` for Postgres(pgvector) + credentials via `.env`.
3. Scaffold Spring Boot app with Gradle, health controllers, datasource config, Flyway baseline migration.
4. Scaffold Next.js app with minimal page and API base URL from env.
5. Add root `Makefile` commands (`setup`, `dev`, `test`, `lint`) with practical defaults.
6. Update docs: add `docs/PRD.md` placeholder (required by AGENTS), and bootstrap run guide.
7. Verify basic startup commands (if toolchain available in environment).

Validation:
- `docker compose up -d db`
- API starts and `/api/health` returns 200.
- Web starts and renders.

## 6) Risks & rollback
Risks:
- Local environment may miss Java/Node tooling.
- Network-restricted environment may block dependency install.

Rollback:
- Keep bootstrap changes isolated to new paths under `apps/`, `infra/`, `plans/`, and root helper files.
