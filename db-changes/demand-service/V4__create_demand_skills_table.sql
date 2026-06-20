CREATE TABLE IF NOT EXISTS demand_skills (
    id BIGSERIAL PRIMARY KEY,
    demand_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    is_mandatory BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_demand_skills_demand FOREIGN KEY (demand_id) REFERENCES demands (demand_id) ON DELETE CASCADE,
    CONSTRAINT fk_demand_skills_skill FOREIGN KEY (skill_id) REFERENCES skills (skill_id) ON DELETE CASCADE,
    CONSTRAINT uq_demand_skill UNIQUE (demand_id, skill_id)
);
