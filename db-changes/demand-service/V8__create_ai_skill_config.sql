CREATE TABLE ai_skill_suggestion_config (
    id BIGSERIAL PRIMARY KEY,
    mode VARCHAR(30) NOT NULL DEFAULT 'TOP_N_SIMILARITY',
    top_n INTEGER NOT NULL DEFAULT 40,
    updated_by BIGINT,
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CHECK (mode IN ('FULL_CATALOG', 'TOP_N_SIMILARITY'))
);

INSERT INTO ai_skill_suggestion_config (mode, top_n) VALUES ('TOP_N_SIMILARITY', 40);
