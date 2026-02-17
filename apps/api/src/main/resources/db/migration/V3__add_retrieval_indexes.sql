ALTER TABLE chunk
  ADD COLUMN IF NOT EXISTS search_vector tsvector
  GENERATED ALWAYS AS (to_tsvector('simple', coalesce(text, ''))) STORED;

CREATE INDEX IF NOT EXISTS idx_chunk_search_vector ON chunk USING GIN (search_vector);

CREATE INDEX IF NOT EXISTS idx_embedding_vector_ivfflat
  ON embedding
  USING ivfflat (embedding vector_cosine_ops)
  WITH (lists = 100);
