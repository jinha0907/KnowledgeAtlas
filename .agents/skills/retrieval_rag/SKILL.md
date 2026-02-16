---
name: retrieval-rag
description: Use this when implementing search/RAG: embedding query, pgvector similarity search, optional keyword search, and returning answers with citations (document/block references).
---

## Retrieval policy (MVP)
- topK vector search on embedding table joined with chunk/content_block.
- Return: chunks + doc title + block reference so UI can jump to evidence.
- Keep response deterministic: include score, document_id, block_id.

## Guardrails
- Never hallucinate citations. If no evidence, say so and return empty citations.
