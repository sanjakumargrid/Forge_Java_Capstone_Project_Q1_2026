-- Drop the old 384 dimension index and column
DROP INDEX IF EXISTS idx_skills_embedding;
ALTER TABLE skills DROP COLUMN IF EXISTS embedding;

-- Add the new 768 dimension column for Gemini
ALTER TABLE skills ADD COLUMN embedding vector(768);
CREATE INDEX idx_skills_embedding ON skills USING hnsw (embedding vector_cosine_ops);
