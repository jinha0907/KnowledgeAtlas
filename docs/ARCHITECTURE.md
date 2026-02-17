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

## Data flow (happy path)
1) Notion 페이지/DB 변경 감지(증분 동기화) -> raw snapshot 저장
2) 블록 텍스트 정규화 -> chunk 생성
3) chunk embedding 생성 -> pgvector upsert
4) 검색 API: query embedding + (optional) 키워드 검색 -> 결과 + 근거 반환
5) 회의록 처리: 논의/결정 추출 -> Decision 엔티티 + evidence 링크 저장

## Non-goals (MVP)
- Confluence/Jira/GitHub 연동은 이후 단계(플러그인 방식)
- 완전 자동 분류 강제는 하지 않음(반자동 보정 UI 제공)
