# Security & Privacy (MVP)

- 토큰/키는 절대 커밋 금지. 로컬 `.env`만 사용하고 `.env.example`로 공유한다.
- 외부 연동(Notion)은 “복사본 저장”을 기본으로 하며, 원문 링크/식별자는 최소화한다.
- OpenAI embedding/decision extraction은 명시적으로 `openai` provider를 설정한 경우에만 원문 chunk를 외부 API로 전송한다. 기본값은 `none`이며, 비활성화 상태에서는 외부 호출을 하지 않는다.
- 추출된 결정은 자동 수락하지 않는다. 원문 block 인용을 보존하고 단일 사용자가 검토 후 `accepted`로 변경한다.
- 접근 제어는 MVP에서 단일 사용자/단일 워크스페이스를 기본으로 하되, 향후 멀티테넌시 확장을 고려해 tenant_id 컬럼 확장 여지를 남긴다.
