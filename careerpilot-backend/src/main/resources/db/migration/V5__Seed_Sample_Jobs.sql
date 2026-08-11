-- V5__Seed_Sample_Jobs.sql

INSERT INTO jobs (id, title, company, location, description, required_skills, job_url, source)
VALUES 
    (gen_random_uuid(), 'Senior Frontend Engineer', 'TechCorp', 'Remote', 
    'We are looking for an experienced frontend engineer to lead our React and Next.js development.',
    '{"React", "Next.js", "TypeScript", "Tailwind CSS"}', 'https://techcorp.com/jobs/1', 'seed'),

    (gen_random_uuid(), 'Backend Java Developer', 'Fintech Inc', 'New York, NY',
    'Join our payments team building robust microservices with Spring Boot.',
    '{"Java", "Spring Boot", "PostgreSQL", "Redis", "Microservices"}', 'https://fintech.com/jobs/42', 'seed'),

    (gen_random_uuid(), 'Data Scientist', 'AI Innovations', 'San Francisco, CA',
    'Looking for a data scientist to build machine learning models for user behavior prediction.',
    '{"Python", "Machine Learning", "SQL", "TensorFlow", "Pandas"}', 'https://ai-innovations.com/careers/data', 'seed'),

    (gen_random_uuid(), 'Product Designer', 'Creative Solutions', 'Remote',
    'We need a UX/UI designer to revamp our core product dashboard.',
    '{"Figma", "UX Design", "UI Design", "Prototyping", "User Research"}', 'https://creative.com/jobs/designer', 'seed'),

    (gen_random_uuid(), 'DevOps Engineer', 'CloudScale', 'Austin, TX',
    'Help us scale our infrastructure using Kubernetes, Terraform, and AWS.',
    '{"Kubernetes", "AWS", "Terraform", "CI/CD", "Docker"}', 'https://cloudscale.io/careers/devops', 'seed');
