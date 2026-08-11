-- ============================================================
-- V3__Add_Application_Kanban_Fields.sql
-- F5: Job Application Tracking Board (Kanban)
-- Extends the applications table in-place. V1 and V2 must already be applied.
-- DO NOT modify V1__Initial_Schema.sql or V2__Add_Resume_Generation.sql.
-- ============================================================

-- 1. Nullable FK to the tailored resume used for this application (F6 integration)
ALTER TABLE applications
    ADD COLUMN resume_id UUID REFERENCES resumes(id) ON DELETE SET NULL;

-- 2. Timestamp recording when the status last changed (audit trail + timeline UI)
ALTER TABLE applications
    ADD COLUMN status_changed_at TIMESTAMP;

-- Backfill: treat created_at as the initial status timestamp for existing rows
UPDATE applications
    SET status_changed_at = created_at
    WHERE status_changed_at IS NULL;

-- 3. Optimistic locking version counter (prevents lost-update on concurrent Kanban drags)
ALTER TABLE applications
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- 4. Composite index: supports the status-filtered list endpoint and board group-by
CREATE INDEX idx_applications_user_status
    ON applications(user_id, status);

-- 5. Composite index: supports the default sort (createdAt DESC) on the list endpoint
CREATE INDEX idx_applications_user_created
    ON applications(user_id, created_at DESC);
