ALTER TABLE index_jobs ADD COLUMN job_type varchar(16) NOT NULL DEFAULT 'INDEX';
ALTER TABLE index_jobs ADD COLUMN attempts integer NOT NULL DEFAULT 0;
ALTER TABLE index_jobs ADD COLUMN started_at timestamptz;
CREATE INDEX idx_index_jobs_pending ON index_jobs(status, created_at);
