-- Mock Data for Demand Service Entities
-- Use this script to populate test data for demands, skills, job titles, etc.

-- 1. Insert Job Titles
INSERT INTO job_titles (title_name) VALUES
    ('Software Engineer'),
    ('Senior Software Engineer'),
    ('DevOps Engineer'),
    ('Data Scientist'),
    ('Product Manager')
ON CONFLICT (title_name) DO NOTHING;

-- 2. Insert Skills (embedding left as null for simplicity)
INSERT INTO skills (skill_name, embedding_updated_at) VALUES
    ('Java', CURRENT_TIMESTAMP),
    ('Spring Boot', CURRENT_TIMESTAMP),
    ('PostgreSQL', CURRENT_TIMESTAMP),
    ('Kubernetes', CURRENT_TIMESTAMP),
    ('Docker', CURRENT_TIMESTAMP),
    ('Python', CURRENT_TIMESTAMP),
    ('React', CURRENT_TIMESTAMP),
    ('Angular', CURRENT_TIMESTAMP),
    ('AWS', CURRENT_TIMESTAMP),
    ('Terraform', CURRENT_TIMESTAMP)
ON CONFLICT (skill_name) DO NOTHING;

-- 3. Insert Demands
-- Demand 1: A draft demand for a Senior Java Developer
INSERT INTO demands (
    title, description, level, employment_type, location,
    account_name, account_id, project_name, project_id, business_unit,
    job_title_id, budget, req_util_perc, work_mode, experience,
    department, client_interview, onboarding_date, is_filled, bench_hiring,
    status, priority, target_date, created_by, creator_name,
    creator_email, is_deleted, version, created_at, updated_at, approval_reminder_sent
) VALUES (
    'Senior Java Developer', 'Looking for an experienced Java dev for the backend team.', 'SENIOR', 'FULL_TIME', 'New York',
    'Acme Corp', 101, 'Acme Platform Rewrite', 1001, 'Engineering',
    (SELECT job_title_id FROM job_titles WHERE title_name = 'Senior Software Engineer'), 120000.00, 100, 'HYBRID', 5,
    'Backend Integration', true, CURRENT_DATE + INTERVAL '30 days', false, false,
    'DRAFT', 'HIGH', CURRENT_DATE + INTERVAL '45 days', 10, 'Alice Manager',
    'alice@example.com', false, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false
);

-- Demand 2: An approved demand for a DevOps Engineer
INSERT INTO demands (
    title, description, level, employment_type, location,
    account_name, account_id, project_name, project_id, business_unit,
    job_title_id, budget, req_util_perc, work_mode, experience,
    department, client_interview, onboarding_date, is_filled, bench_hiring,
    status, priority, target_date, created_by, creator_name,
    creator_email, approved_by, approver_name, approved_at,
    is_deleted, version, created_at, updated_at, approval_reminder_sent
) VALUES (
    'DevOps Engineer - Kubernetes', 'Kubernetes cluster management and CI/CD pipelines.', 'MID_LEVEL', 'FULL_TIME', 'Remote',
    'Globex', 102, 'Cloud Migration', 1002, 'Platform',
    (SELECT job_title_id FROM job_titles WHERE title_name = 'DevOps Engineer'), 100000.00, 100, 'REMOTE', 3,
    'Cloud Ops', false, CURRENT_DATE + INTERVAL '15 days', false, false,
    'APPROVED', 'CRITICAL', CURRENT_DATE + INTERVAL '20 days', 11, 'Bob Director',
    'bob@example.com', 12, 'Charlie VP', CURRENT_TIMESTAMP,
    false, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false
);

-- Demand 3: A filled demand for a Data Scientist
INSERT INTO demands (
    title, description, level, employment_type, location,
    account_name, account_id, project_name, project_id, business_unit,
    job_title_id, budget, req_util_perc, work_mode, experience,
    department, client_interview, onboarding_date, is_filled, fill_type, bench_hiring,
    status, priority, target_date, created_by, creator_name,
    creator_email, approved_by, approver_name, approved_at,
    is_deleted, version, created_at, updated_at, approval_reminder_sent
) VALUES (
    'Data Scientist', 'Machine learning and predictive modeling.', 'MID_LEVEL', 'FULL_TIME', 'San Francisco',
    'Initech', 103, 'Analytics Revamp', 1003, 'Data Science',
    (SELECT job_title_id FROM job_titles WHERE title_name = 'Data Scientist'), 140000.00, 80, 'HYBRID', 4,
    'Data Insights', true, CURRENT_DATE - INTERVAL '5 days', true, 'INTERNAL', false,
    'FILLED', 'MEDIUM', CURRENT_DATE, 13, 'Diana HR',
    'diana@example.com', 14, 'Eve Head', CURRENT_TIMESTAMP - INTERVAL '10 days',
    false, 2, CURRENT_TIMESTAMP - INTERVAL '20 days', CURRENT_TIMESTAMP, false
);

-- 4. Insert Demand Skills
-- Demand 1 Skills (Java, Spring Boot, PostgreSQL)
INSERT INTO demand_skills (demand_id, skill_id, is_mandatory) VALUES
    ((SELECT demand_id FROM demands WHERE title = 'Senior Java Developer'), (SELECT skill_id FROM skills WHERE skill_name = 'Java'), true),
    ((SELECT demand_id FROM demands WHERE title = 'Senior Java Developer'), (SELECT skill_id FROM skills WHERE skill_name = 'Spring Boot'), true),
    ((SELECT demand_id FROM demands WHERE title = 'Senior Java Developer'), (SELECT skill_id FROM skills WHERE skill_name = 'PostgreSQL'), false);

-- Demand 2 Skills (Kubernetes, Docker, AWS, Terraform)
INSERT INTO demand_skills (demand_id, skill_id, is_mandatory) VALUES
    ((SELECT demand_id FROM demands WHERE title = 'DevOps Engineer - Kubernetes'), (SELECT skill_id FROM skills WHERE skill_name = 'Kubernetes'), true),
    ((SELECT demand_id FROM demands WHERE title = 'DevOps Engineer - Kubernetes'), (SELECT skill_id FROM skills WHERE skill_name = 'Docker'), true),
    ((SELECT demand_id FROM demands WHERE title = 'DevOps Engineer - Kubernetes'), (SELECT skill_id FROM skills WHERE skill_name = 'AWS'), true),
    ((SELECT demand_id FROM demands WHERE title = 'DevOps Engineer - Kubernetes'), (SELECT skill_id FROM skills WHERE skill_name = 'Terraform'), false);

-- Demand 3 Skills (Python)
INSERT INTO demand_skills (demand_id, skill_id, is_mandatory) VALUES
    ((SELECT demand_id FROM demands WHERE title = 'Data Scientist'), (SELECT skill_id FROM skills WHERE skill_name = 'Python'), true);

-- 5. Insert Demand Status History
-- Demand 2 (Draft -> Pending -> Approved)
INSERT INTO demand_status_history (demand_id, from_status, to_status, changed_by, comments, changed_at) VALUES
    ((SELECT demand_id FROM demands WHERE title = 'DevOps Engineer - Kubernetes'), 'DRAFT', 'PENDING_APPROVAL', 11, 'Submitting for VP approval.', CURRENT_TIMESTAMP - INTERVAL '2 days'),
    ((SELECT demand_id FROM demands WHERE title = 'DevOps Engineer - Kubernetes'), 'PENDING_APPROVAL', 'APPROVED', 12, 'Approved, budget is clear.', CURRENT_TIMESTAMP);

-- Demand 3 (Draft -> Pending -> Approved -> Internal Search -> Filled)
INSERT INTO demand_status_history (demand_id, from_status, to_status, changed_by, comments, changed_at) VALUES
    ((SELECT demand_id FROM demands WHERE title = 'Data Scientist'), 'DRAFT', 'PENDING_APPROVAL', 13, 'Submitting data science role.', CURRENT_TIMESTAMP - INTERVAL '15 days'),
    ((SELECT demand_id FROM demands WHERE title = 'Data Scientist'), 'PENDING_APPROVAL', 'APPROVED', 14, 'Approved.', CURRENT_TIMESTAMP - INTERVAL '10 days'),
    ((SELECT demand_id FROM demands WHERE title = 'Data Scientist'), 'APPROVED', 'INTERNAL_SEARCH', 15, 'Starting search on bench.', CURRENT_TIMESTAMP - INTERVAL '9 days'),
    ((SELECT demand_id FROM demands WHERE title = 'Data Scientist'), 'INTERNAL_SEARCH', 'FILLED', 16, 'Filled with internal candidate X.', CURRENT_TIMESTAMP - INTERVAL '1 days');
