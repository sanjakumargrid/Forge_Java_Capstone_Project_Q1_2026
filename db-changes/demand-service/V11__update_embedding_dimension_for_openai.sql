ALTER TABLE skills DROP COLUMN IF EXISTS embedding;
ALTER TABLE skills ADD COLUMN embedding vector(1536);
CREATE INDEX IF NOT EXISTS idx_skills_embedding_hnsw
    ON skills USING hnsw (embedding vector_cosine_ops);
