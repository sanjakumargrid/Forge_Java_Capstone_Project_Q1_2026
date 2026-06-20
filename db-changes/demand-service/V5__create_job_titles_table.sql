CREATE TABLE IF NOT EXISTS job_titles (
    job_title_id BIGSERIAL PRIMARY KEY,
    title_name VARCHAR(255) NOT NULL UNIQUE
);

INSERT INTO job_titles (title_name) VALUES 
('Software Engineer'),
('Senior Software Engineer'),
('Lead Software Engineer'),
('Principal Software Engineer'),
('Software Architect'),
('Frontend Engineer'),
('Backend Engineer'),
('Full Stack Engineer'),
('DevOps Engineer'),
('Site Reliability Engineer (SRE)'),
('Data Engineer'),
('Machine Learning Engineer'),
('Cloud Engineer'),
('Security Engineer'),
('QA Engineer')
ON CONFLICT (title_name) DO NOTHING;

ALTER TABLE demands ADD COLUMN job_title_id BIGINT;
ALTER TABLE demands ADD CONSTRAINT fk_demands_job_title FOREIGN KEY (job_title_id) REFERENCES job_titles (job_title_id);
