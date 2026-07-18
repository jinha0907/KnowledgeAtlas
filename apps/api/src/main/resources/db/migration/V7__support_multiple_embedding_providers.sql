DROP INDEX IF EXISTS idx_embedding_vector_ivfflat;

ALTER TABLE embedding
  ADD COLUMN IF NOT EXISTS provider TEXT,
  ADD COLUMN IF NOT EXISTS dimensions INTEGER;

UPDATE embedding
SET provider = COALESCE(provider, 'openai'),
    dimensions = COALESCE(dimensions, 1536);

ALTER TABLE embedding
  ALTER COLUMN provider SET NOT NULL,
  ALTER COLUMN dimensions SET NOT NULL,
  ALTER COLUMN embedding TYPE vector USING embedding::vector;

ALTER TABLE embedding
  ADD CONSTRAINT chk_embedding_dimensions_positive CHECK (dimensions > 0);

CREATE INDEX idx_embedding_vector_1536_ivfflat
  ON embedding
  USING ivfflat ((embedding::vector(1536)) vector_cosine_ops)
  WITH (lists = 100)
  WHERE dimensions = 1536;
