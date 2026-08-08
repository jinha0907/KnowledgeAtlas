# Architecture (MVP)

## Components
- Notion Sync Worker (Spring @Scheduled / 또는 Job runner)
- Normalizer/Chunker
- Embedding Upserter (pgvector)
- Retrieval API (검색 + 근거 반환)
- Decision Extractor (회의록 -> 논의/결정/근거 연결)
- Web UI (Project Map / Decision Map)

## Implemented API surface (Phase 3)
- `POST /api/search`
  - keyword/FTS 기반 retrieval, deterministic ordering (`score DESC, chunk_id ASC`)
  - response includes evidence citation fields: `score`, `documentId`, `blockId`, `title`, `text`
- `POST /api/decisions`
- `GET /api/decisions`
- `GET /api/decisions/{id}`
- `PATCH /api/decisions/{id}/status`
- `POST /api/decisions/{id}/evidence`

## Implemented API surface (Phase 4)
- `POST /api/notion/sync/run`
  - real Notion API 호출 후 증분 동기화 실행
  - 기준 시점: 마지막 성공한 `sync_job_run.source_watermark_at`의 2분 전
  - 중복 실행은 `409 Conflict`로 거절하고, 결과 집합이 `maxPages`를 넘으면 워터마크를 진행시키지 않고 실패 처리
  - Notion 호출의 네트워크/429/5xx 실패는 retry/backoff 후 실패 상태 기록

## Implemented API surface (Phase 5)
- `POST /api/search`
  - `EMBEDDING_PROVIDER=openai` 설정 시 OpenAI query embedding과 PostgreSQL FTS 후보를 reciprocal-rank fusion으로 결합
  - 제공자가 없거나 호출에 실패하면 FTS 검색으로 폴백하며, 모든 결과에는 문서·블록 근거를 유지
- `POST /api/embeddings/backfill`
  - embedding이 없는 기존 chunk만 찾아 배치 생성; 제공자가 미설정이면 `disabled` 응답

## Implemented API surface (Phase 6)
- `POST /api/documents/{documentId}/decisions/extract`
  - optional OpenAI extractor가 문서 block에서 논의·결과·confidence·정확한 근거 인용을 추출
  - extractor가 비활성화된 경우 외부 호출 없이 `disabled`를 반환
  - 같은 source checksum은 기존 run의 후보를 반환하고, 결과는 항상 `proposed` 상태로만 생성

## Implemented API surface (Phase 7)
- `GET /api/documents`
  - 동기화된 문서의 식별자, source, 제목, 마지막 동기화 시각을 최신 순으로 반환
- `GET /api/documents/{documentId}`
  - 선택한 문서와 저장된 Notion block을 경로 순으로 반환
- Web UI (`apps/web`)
  - 문서 목록, 결정 상태별 map, 결정의 block-level evidence, 선택 문서의 저장 블록을 같은 화면에서 제공
  - 동기화, embedding backfill, decision extraction을 기존 API에 연결

## Implemented API surface (Phase 8)
- `POST /api/documents/{documentId}/analysis/run`
  - optional OpenAI provider가 저장된 block에서 요약과 최대 8개 태그를 생성
  - provider가 비활성화되면 외부 호출 없이 `disabled`를 반환하며, 같은 source checksum은 기존 성공 결과를 반환
- Document read API와 Atlas
  - 현재 source checksum의 analysis run success/running/failed 상태와 성공한 summary/tags를 함께 반환하고 표시

## Implemented API surface (Phase 9)
- `GET /api/project-graph`
  - stable `document-{id}`, `decision-{id}`, `evidence-{id}` 노드와 deterministic edge를 반환
  - 모든 edge는 persisted `decision_evidence` 행의 `decision -> evidence -> document/block` 경로에서만 파생
- Atlas graph
  - SVG edge와 keyboard-operable node를 사용하며, 상태 필터가 선택한 decision의 evidence/document 경로를 함께 유지

## Implemented API surface (Phase 10)
- Atlas citation-first search
  - 기존 `POST /api/search`를 사용해 title, score, document ID, block ID, text로 구성된 citation을 표시
  - 결과 선택 시 해당 로컬 문서와 block을 source inspector에서 연다

## Implemented API surface (Phase 11)
- Local Ollama providers
  - `EMBEDDING_PROVIDER=ollama` calls a local Ollama `/api/embed` endpoint without an API key.
  - `DOCUMENT_ANALYSIS_PROVIDER=ollama` and `DECISION_EXTRACTION_PROVIDER=ollama` use local `/api/chat` JSON responses.
  - Providers never silently fall back from Ollama to OpenAI; inference failure leaves source data intact and search falls back to FTS.
  - Local analysis and extraction use a deterministic whole-block prompt prefix, disable Qwen thinking mode, require JSON Schema output, and bound context/output so a large document cannot create an unbounded local request.
- `POST /api/embeddings/reindex`
  - requires `{ "confirm": true }`, deletes only persisted embedding rows, and regenerates them with the currently configured provider.
  - a provider/model/dimension mismatch blocks embedding backfill; hybrid search falls back to FTS until re-indexing is explicitly confirmed.

## Implemented API surface (Phase 12)
- `GET /api/embeddings/status`
  - returns configured provider identity, persisted identities, eligible/embedded/missing chunk counts, and whether re-indexing is required.
  - performs no model inference and never contacts Ollama or OpenAI.
- Atlas retrieval readiness
  - shows provider state before semantic retrieval and requires an explicit checkbox before it calls the destructive re-index API.

## Implemented API surface (Phase 13)
- Atlas decision review
  - shows the selected decision's persisted status beside its evidence.
  - offers only valid lifecycle transitions: evidence-backed `proposed -> accepted` and `accepted -> obsolete`.
  - optionally links an obsolete decision to another accepted decision through the existing `supersedesDecisionId` field.
- Web-to-API development boundary
  - Atlas calls same-origin `/api/*`; Next.js rewrites requests server-side to `API_BASE_URL` (default `http://localhost:8080`).
  - This avoids exposing the backend base URL to the browser and keeps local browser port policies from breaking the Atlas.

## Implemented API surface (Phase 14)
- `POST /api/knowledge-graph/rebuild`
  - explicitly rebuilds the derived document similarity graph from chunks with the active embedding identity.
  - removes and replaces only `document_similarity`; source documents, chunks, embeddings, and decisions are unchanged.
  - retains sparse top-neighbour edges above the configured rebuild score and returns document/edge counts.
- `GET /api/knowledge-graph?minimumScore=0.35`
  - returns stored document nodes and current-identity similarity edges with two representative chunk citations.
  - suppresses stale rows when a source checksum or embedding identity changed; callers receive `rebuild_required` if rows no longer match.
- Atlas semantic graph
  - defaults to an SVG document constellation with pan, zoom, node drag, score filtering, document opening, and edge-evidence inspection.
  - retains the existing decision graph as a separate review mode because semantic relatedness is not decision evidence.

## Data flow (happy path)
1) Notion 페이지/블록 변경 감지(증분 동기화) -> canonical title/block content checksum으로 무변경 snapshot skip -> 변경 시에만 페이지와 재귀 블록 raw snapshot 저장 및 삭제된 블록 정리
2) 블록 텍스트 정규화 -> chunk 생성
3) 변경된 chunk만 active provider embedding 생성 -> pgvector upsert (기존 데이터는 수동 backfill 가능)
4) 검색 API: query embedding + 키워드 FTS를 deterministic RRF로 결합 -> 결과 + 근거 반환
5) 회의록 처리: 논의/결정 추출 -> Decision 엔티티 + evidence 링크 저장
6) Web UI: 문서/결정 read API를 조회 -> evidence가 가리키는 로컬 문서 block을 열어 검토
7) Document analysis: 저장 block -> optional provider -> checksum-idempotent summary/tags -> Atlas에서 검토
8) Project graph: documents + decisions + decision evidence -> deterministic node/edge API -> Atlas graph에서 근거 추적
9) Knowledge search: user query -> retrieval API -> cited block results -> Atlas source inspector
10) Knowledge graph: explicit rebuild -> document-centroid candidate links -> sparse persisted similarity edges + representative chunks -> Atlas graph/evidence drawer

## Local model operating rule
- The active embedding identity is `(provider, model, dimensions)`. A corpus has exactly one active identity at a time.
- Switching identity requires a confirmed re-index because cosine distances from different vector spaces must never be mixed.
- The migration keeps a partial ivfflat index for current 1536-dimensional vectors. Other dimensions remain correct and use FTS plus sequential vector candidates until a corpus-size-specific index is introduced.

## Retrieval evaluation rule
- Copy `docs/retrieval-evaluation.template.json` to an ignored local result file and fill only evaluator-supplied queries plus expected local document/block IDs.
- Run each query through `POST /api/search` after a confirmed re-index, then record returned document/block rank and latency. Do not compare raw similarity scores across providers.

## Non-goals (MVP)
- Confluence/Jira/GitHub 연동은 이후 단계(플러그인 방식)
- 완전 자동 분류 강제는 하지 않음(반자동 보정 UI 제공)

## Validation and CI (Phase 4.1)
- Unit tests run without a local database; PostgreSQL/pgvector integration tests use Testcontainers and verify Flyway migrations plus the critical Notion persistence path.
- GitHub Actions runs API tests on Temurin Java 21 and runs the web lint/build checks on Node 22 for pushes and pull requests.
