CREATE EXTENSION IF NOT EXISTS vector;

ALTER TABLE skills ADD COLUMN embedding vector(384);
ALTER TABLE skills ADD COLUMN embedding_updated_at TIMESTAMP;

CREATE INDEX idx_skills_embedding ON skills USING hnsw (embedding vector_cosine_ops);
