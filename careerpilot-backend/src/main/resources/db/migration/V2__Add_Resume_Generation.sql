-- ============================================================
-- V2__Add_Resume_Generation.sql
-- F6: JD-Matched ATS Resume Generator
-- Extends the resumes table in-place. V1 must already be applied.
-- DO NOT modify V1__Initial_Schema.sql (already applied to prod/staging).
-- ============================================================

-- 1. Discriminator: 'UPLOADED' (default) or 'GENERATED'
ALTER TABLE resumes
    ADD COLUMN source_type VARCHAR(20) NOT NULL DEFAULT 'UPLOADED';

UPDATE resumes SET source_type = 'UPLOADED' WHERE source_type IS NULL;

-- 2. FK to the job this resume was tailored for (NULL for UPLOADED resumes)
ALTER TABLE resumes
    ADD COLUMN source_job_id UUID REFERENCES jobs(id) ON DELETE SET NULL;

-- 3. R2 key for the generated PDF output (NULL for UPLOADED resumes)
ALTER TABLE resumes
    ADD COLUMN file_key_pdf VARCHAR(500);

-- 4. R2 key for the generated DOCX output (NULL for UPLOADED resumes)
ALTER TABLE resumes
    ADD COLUMN file_key_docx VARCHAR(500);

-- 5. Canonical JSON resume document produced by OpenAI
--    NULL for UPLOADED resumes; non-null for GENERATED once DONE.
ALTER TABLE resumes
    ADD COLUMN generated_content JSONB;

-- 6. JD keywords the AI could NOT honestly match to the candidate's experience.
--    Surfaced in the UI as a "Gap Report".
ALTER TABLE resumes
    ADD COLUMN unmatched_keywords TEXT[];

-- 7. Relax NOT NULL on file_key and original_filename so GENERATED rows
--    can be inserted without these fields (they use generated_content instead).
ALTER TABLE resumes ALTER COLUMN file_key DROP NOT NULL;
ALTER TABLE resumes ALTER COLUMN original_filename DROP NOT NULL;

-- 8. Partial index for efficient lookup of all generated resumes per user/job
CREATE INDEX idx_resumes_generated_by_user_job
    ON resumes(user_id, source_job_id)
    WHERE source_type = 'GENERATED';
