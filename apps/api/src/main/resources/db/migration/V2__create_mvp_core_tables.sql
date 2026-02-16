CREATE TABLE IF NOT EXISTS source_document (
  id BIGSERIAL PRIMARY KEY,
  source_type TEXT NOT NULL,
  source_id TEXT NOT NULL,
  title TEXT,
  last_synced_at TIMESTAMPTZ NOT NULL,
  raw_json JSONB NOT NULL,
  checksum TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (source_type, source_id)
);

CREATE TABLE IF NOT EXISTS content_block (
  id BIGSERIAL PRIMARY KEY,
  document_id BIGINT NOT NULL REFERENCES source_document(id) ON DELETE CASCADE,
  block_id TEXT NOT NULL,
  text TEXT NOT NULL,
  path TEXT,
  updated_at TIMESTAMPTZ NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (document_id, block_id)
);

CREATE TABLE IF NOT EXISTS chunk (
  id BIGSERIAL PRIMARY KEY,
  document_id BIGINT NOT NULL REFERENCES source_document(id) ON DELETE CASCADE,
  block_id TEXT NOT NULL,
  chunk_index INTEGER NOT NULL,
  text TEXT NOT NULL,
  token_count INTEGER NOT NULL,
  checksum TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (document_id, block_id, chunk_index)
);

CREATE TABLE IF NOT EXISTS embedding (
  chunk_id BIGINT PRIMARY KEY REFERENCES chunk(id) ON DELETE CASCADE,
  embedding VECTOR(1536) NOT NULL,
  model TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS decision (
  id BIGSERIAL PRIMARY KEY,
  title TEXT NOT NULL,
  status TEXT NOT NULL CHECK (status IN ('proposed', 'accepted', 'obsolete')),
  outcome TEXT NOT NULL,
  supersedes_decision_id BIGINT REFERENCES decision(id) ON DELETE SET NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS decision_evidence (
  id BIGSERIAL PRIMARY KEY,
  decision_id BIGINT NOT NULL REFERENCES decision(id) ON DELETE CASCADE,
  document_id BIGINT NOT NULL REFERENCES source_document(id) ON DELETE CASCADE,
  block_id TEXT NOT NULL,
  quote TEXT NOT NULL,
  rationale TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS sync_job_run (
  id BIGSERIAL PRIMARY KEY,
  source_type TEXT NOT NULL,
  started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  finished_at TIMESTAMPTZ,
  status TEXT NOT NULL CHECK (status IN ('running', 'success', 'failed')),
  synced_documents INTEGER NOT NULL DEFAULT 0,
  error_message TEXT
);

CREATE INDEX IF NOT EXISTS idx_content_block_document ON content_block(document_id);
CREATE INDEX IF NOT EXISTS idx_chunk_document ON chunk(document_id);
CREATE INDEX IF NOT EXISTS idx_chunk_checksum ON chunk(checksum);
CREATE INDEX IF NOT EXISTS idx_decision_status ON decision(status);
CREATE INDEX IF NOT EXISTS idx_decision_evidence_decision ON decision_evidence(decision_id);
