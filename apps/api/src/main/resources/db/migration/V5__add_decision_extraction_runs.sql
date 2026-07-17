ALTER TABLE decision
  ADD COLUMN IF NOT EXISTS discussion TEXT,
  ADD COLUMN IF NOT EXISTS confidence DOUBLE PRECISION CHECK (confidence >= 0.0 AND confidence <= 1.0),
  ADD COLUMN IF NOT EXISTS extraction_run_id BIGINT;

CREATE TABLE IF NOT EXISTS decision_extraction_run (
  id BIGSERIAL PRIMARY KEY,
  document_id BIGINT NOT NULL REFERENCES source_document(id) ON DELETE CASCADE,
  source_checksum TEXT NOT NULL,
  status TEXT NOT NULL CHECK (status IN ('running', 'success', 'failed')),
  extracted_decisions INTEGER NOT NULL DEFAULT 0,
  error_message TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  completed_at TIMESTAMPTZ,
  UNIQUE (document_id, source_checksum)
);

ALTER TABLE decision
  ADD CONSTRAINT fk_decision_extraction_run
  FOREIGN KEY (extraction_run_id) REFERENCES decision_extraction_run(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_decision_extraction_run_document
  ON decision_extraction_run (document_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_decision_extraction_run_id
  ON decision (extraction_run_id);
