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

## Data flow (happy path)
1) Notion 페이지/블록 변경 감지(증분 동기화) -> 페이지와 재귀 블록 raw snapshot 저장 -> 삭제된 블록 정리
2) 블록 텍스트 정규화 -> chunk 생성
3) 변경된 chunk만 embedding 생성 -> pgvector upsert (기존 데이터는 수동 backfill 가능)
4) 검색 API: query embedding + 키워드 FTS를 deterministic RRF로 결합 -> 결과 + 근거 반환
5) 회의록 처리: 논의/결정 추출 -> Decision 엔티티 + evidence 링크 저장

## Non-goals (MVP)
- Confluence/Jira/GitHub 연동은 이후 단계(플러그인 방식)
- 완전 자동 분류 강제는 하지 않음(반자동 보정 UI 제공)

## Validation and CI (Phase 4.1)
- Unit tests run without a local database; PostgreSQL/pgvector integration tests use Testcontainers and verify Flyway migrations plus the critical Notion persistence path.
- GitHub Actions runs API tests on Temurin Java 21 and runs the web lint/build checks on Node 22 for pushes and pull requests.
