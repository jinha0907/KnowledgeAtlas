# Repository Guidelines

## Project Structure & Module Organization
This repository is currently minimal (`/Users/jinha/toy`) and has no committed source files yet. Keep the layout predictable as code is added:
- `src/` for application code
- `tests/` for automated tests mirroring `src/`
- `assets/` for static files (images, fixtures, sample data)
- `docs/` for design notes and architecture decisions

Example: `src/auth/session.js` should have related tests in `tests/auth/session.test.js`.

## Build, Test, and Development Commands
Because no build system is committed yet, standardize scripts early and expose them through one entrypoint (for example, a `Makefile` or package scripts).
Recommended baseline commands:
- `make setup` to install dependencies/tools
- `make test` to run the full test suite
- `make lint` to run static checks and formatting validation
- `make dev` to start a local development workflow

If your stack uses `npm`, mirror these as `npm run test`, `npm run lint`, etc.

## Coding Style & Naming Conventions
Use consistent formatting and enforce it with tooling.
- Indentation: 2 spaces for JSON/YAML/Markdown; language-default elsewhere
- Naming: `snake_case` for directories/files, `PascalCase` for classes/types, `camelCase` for variables/functions
- Keep modules focused; prefer small files over multi-purpose utilities

Adopt a formatter/linter (for example, Prettier + ESLint or Black + Ruff) and run it before opening PRs.

## Testing Guidelines
Place tests under `tests/` with names matching the source file plus `.test` or `_test` (stack-dependent).
- Unit tests should cover normal flow, edge cases, and error handling
- Add regression tests for every bug fix
- Aim for meaningful coverage on changed code, not just global percentage

Run tests locally before pushing changes.

## Commit & Pull Request Guidelines
No repository history exists yet, so use Conventional Commits moving forward:
- `feat: add session timeout handling`
- `fix: prevent null token crash`
- `docs: update setup steps`

PRs should include:
- Clear summary of what changed and why
- Linked issue/ticket when available
- Test evidence (command + result)
- Screenshots/logs for UI or behavior changes

# Project: Project Knowledge Graph + Decision Tracker (MVP)

## Goal (MVP)
- Notion(우선)에서 문서/회의록을 복사본으로 동기화해 Postgres에 저장한다.
- 자동 요약/태깅을 통해 문서 분류를 만든다.
- 회의록에서 "논의 사항 + 결과(결정)"를 추출해 결정(Decision) 단위로 트래킹한다.
- pgvector 기반 검색 + 근거 반환(RAG 형태)을 제공한다.
- (FE) 프로젝트 맵/결정 맵을 그래프로 시각화한다.

## Working agreements (Codex)
- 작업 시작 시 항상 `docs/PRD.md`, `docs/ARCHITECTURE.md`, `docs/DATA_MODEL.md`를 먼저 읽고, 필요한 경우에만 수정한다.
- 큰 작업(새 모듈 도입/대규모 리팩터/스키마 변경)은 먼저 ExecPlan을 만든다:
  - ExecPlan은 `PLANS.md` 규칙을 따르는 `plans/<topic>.md`로 작성한다.
- 비밀키/토큰/사내 데이터는 절대 커밋하지 않는다. `.env.example`만 갱신한다.
- 변경이 DB 스키마/동기화/검색 동작에 영향을 주면 반드시 문서도 업데이트한다(`docs/*`).

## Repo layout
- FE: `apps/web` (Next.js)
- BE: `apps/api` (Spring Boot)
- Shared docs: `docs/`
- Codex skills: `.agents/skills/`

## Definition of done (MVP 단위)
- 로컬에서 `apps/api`가 실행되고, DB 마이그레이션/스키마가 재현 가능하다.
- Notion 테스트 워크스페이스로 최소 1개 DB/페이지를 동기화 → 청킹 → pgvector upsert까지 동작한다.
- 검색 API는 topK 결과와 근거(문서/블록 링크)를 함께 반환한다.
- 결정(Decision)은 회의록 근거와 연결되어 조회 가능하다.

