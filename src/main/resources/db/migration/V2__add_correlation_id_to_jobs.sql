ALTER TABLE jobs
    ADD COLUMN correlation_id VARCHAR(100);

CREATE INDEX idx_jobs_correlation_id
    ON jobs (correlation_id);