# Tech Stack (MVP)

## Chosen
- FE: Next.js
- BE: Spring Boot
- DB: PostgreSQL + pgvector

## Why this stack (non-handwavy)
- Spring Boot:
  - 도메인(결정/근거/권한/추적) 중심 설계와 트랜잭션 경계가 명확
  - 배치/스케줄(동기화), 테스트/계층 구조를 안정적으로 가져가기 쉬움
- PostgreSQL + pgvector:
  - MVP에서 단일 DB로 메타데이터 + 임베딩을 함께 관리 가능(운영/배포 단순)
  - 규모가 커지면 벡터 전용 DB(Qdrant 등)로 분리하는 경로가 열려 있음
- Next.js:
  - 그래프/검색 UX를 빠르게 만들고, 서버 컴포넌트/ISR 등 확장 옵션 보유

## Alternatives considered
- BE: Go / Node(Nest)
- Vector: Qdrant/Milvus/Weaviate
- Search: OpenSearch(FTS 분리)

## Trade-offs (honest)
- pgvector는 대규모/복잡 필터링에서 전용 벡터DB 대비 상한이 낮을 수 있음 → MVP 이후 분리 가능하도록 추상화
- Spring은 초기 부트스트랩 비용이 Node 대비 높을 수 있음 → 생성기/템플릿화로 상쇄
