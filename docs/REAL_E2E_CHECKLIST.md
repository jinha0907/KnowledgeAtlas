# Real External E2E Checklist

Use this checklist only with a user-approved Notion test page containing non-sensitive content. Do not commit `.env` or paste token values into issues, logs, or screenshots.

## 1. Start Local Services

```bash
make setup
make dev-db
make dev-api   # separate terminal
make dev-web   # separate terminal
```

Verify the API and database before calling external services:

```bash
curl -s http://localhost:8080/api/health
curl -s http://localhost:8080/api/db/health
```

## 2. Notion Sync Approval Gate

Set `NOTION_TOKEN` in the ignored local `.env`. Share the target test page with the integration, then run one bounded sync:

```bash
curl -s -X POST http://localhost:8080/api/notion/sync/run \
  -H 'Content-Type: application/json' \
  -d '{"pageSize":20,"maxPages":20}'
```

Confirm copied content and chunks without installing local `psql`:

```bash
docker compose -f infra/docker-compose.yml --env-file .env exec db \
  psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
  -c 'SELECT id, title, last_synced_at FROM source_document ORDER BY id DESC;'
```

## 3. Citation Search and Graph

Replace `QUERY` with text that occurs in the synced test page:

```bash
curl -s -X POST http://localhost:8080/api/search \
  -H 'Content-Type: application/json' \
  -d '{"query":"QUERY","topK":5}'
curl -s http://localhost:8080/api/project-graph
```

Open `http://localhost:3000`. A result must open the cited local document block. If decision evidence exists, the Atlas graph must show `decision -> evidence -> document`.

## 4. Optional OpenAI Approval Gate

Run these only after separate approval. They transmit stored test-page blocks to the configured OpenAI provider and may incur cost:

```bash
# In .env: OPENAI_API_KEY, then one or more provider values below.
EMBEDDING_PROVIDER=openai
DOCUMENT_ANALYSIS_PROVIDER=openai
DECISION_EXTRACTION_PROVIDER=openai
```

Restart `make dev-api` after changing `.env`, then use the synced document ID:

```bash
curl -s -X POST http://localhost:8080/api/embeddings/backfill
curl -s -X POST http://localhost:8080/api/documents/DOCUMENT_ID/analysis/run
curl -s -X POST http://localhost:8080/api/documents/DOCUMENT_ID/decisions/extract
```

Verify that analysis summary/tags appear in the Atlas and extracted decisions remain `proposed` with exact source quotes before accepting any decision.

## 5. Record Outcome

For issue #10, record only command outcomes, document IDs, status values, and redacted error messages. Never record token values or raw private content.
