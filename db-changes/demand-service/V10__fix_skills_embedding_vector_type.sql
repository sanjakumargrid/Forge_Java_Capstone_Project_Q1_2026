-- Hibernate ddl-auto previously created skills.embedding as real[]; pgvector similarity needs vector(768).
DROP INDEX IF EXISTS idx_skills_embedding;
ALTER TABLE skills DROP COLUMN IF EXISTS embedding;
ALTER TABLE skills ADD COLUMN embedding vector(768);
ALTER TABLE skills ADD COLUMN IF NOT EXISTS embedding_updated_at TIMESTAMP;
CREATE INDEX idx_skills_embedding ON skills USING hnsw (embedding vector_cosine_ops);
