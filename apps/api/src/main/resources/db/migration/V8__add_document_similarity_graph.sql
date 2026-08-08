CREATE TABLE document_similarity (
  document_id_low BIGINT NOT NULL REFERENCES source_document(id) ON DELETE CASCADE,
  document_id_high BIGINT NOT NULL REFERENCES source_document(id) ON DELETE CASCADE,
  score DOUBLE PRECISION NOT NULL CHECK (score >= -1.0 AND score <= 1.0),
  chunk_id_low BIGINT NOT NULL REFERENCES chunk(id) ON DELETE CASCADE,
  chunk_id_high BIGINT NOT NULL REFERENCES chunk(id) ON DELETE CASCADE,
  provider TEXT NOT NULL,
  model TEXT NOT NULL,
  dimensions INTEGER NOT NULL CHECK (dimensions > 0),
  source_checksum_low TEXT NOT NULL,
  source_checksum_high TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (document_id_low, document_id_high),
  CHECK (document_id_low < document_id_high)
);

CREATE INDEX idx_document_similarity_identity_score
  ON document_similarity (provider, model, dimensions, score DESC);

CREATE INDEX idx_document_similarity_low ON document_similarity (document_id_low);
CREATE INDEX idx_document_similarity_high ON document_similarity (document_id_high);
