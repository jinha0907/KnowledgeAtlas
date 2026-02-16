# ExecPlans (for Codex)

큰 변경(스키마 변경, 신규 파이프라인/모듈, 대규모 리팩터)은 구현 전에 `plans/<topic>.md`를 작성한다.

## ExecPlan 필수 섹션
1) Purpose / User-visible outcome
2) Scope / Non-goals
3) Data model & migrations (필요 시)
4) API contracts (request/response 예시)
5) Step-by-step implementation plan (검증 방법 포함)
6) Risks & rollback

## 운영 규칙
- 계획은 “살아있는 문서”로 업데이트한다.
- 실행 중 의사결정은 Decision Log에 남긴다.
