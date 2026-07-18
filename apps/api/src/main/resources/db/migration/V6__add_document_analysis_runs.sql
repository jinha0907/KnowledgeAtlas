CREATE TABLE document_analysis_run (
  id BIGSERIAL PRIMARY KEY,
  document_id BIGINT NOT NULL REFERENCES source_document(id) ON DELETE CASCADE,
  source_checksum TEXT NOT NULL,
  status TEXT NOT NULL CHECK (status IN ('running', 'success', 'failed')),
  summary TEXT,
  tags TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
  error_message TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  completed_at TIMESTAMPTZ,
  UNIQUE (document_id, source_checksum),
  CHECK (
    (status = 'success' AND summary IS NOT NULL AND completed_at IS NOT NULL)
    OR status IN ('running', 'failed')
  )
);

CREATE INDEX idx_document_analysis_run_document
  ON document_analysis_run (document_id, created_at DESC);
