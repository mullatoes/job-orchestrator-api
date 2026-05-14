CREATE TABLE jobs (
                      id BIGSERIAL PRIMARY KEY,

                      job_id VARCHAR(50) NOT NULL UNIQUE,
                      job_type VARCHAR(50) NOT NULL,
                      status VARCHAR(30) NOT NULL,

                      payload TEXT NOT NULL,

                      attempt_count INTEGER NOT NULL DEFAULT 0,
                      max_attempts INTEGER NOT NULL DEFAULT 3,

                      error_message TEXT,

                      locked_by VARCHAR(100),
                      locked_at TIMESTAMP,

                      next_retry_at TIMESTAMP,
                      started_at TIMESTAMP,
                      completed_at TIMESTAMP,

                      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                      version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_jobs_job_id
    ON jobs (job_id);

CREATE INDEX idx_jobs_status
    ON jobs (status);

CREATE INDEX idx_jobs_status_next_retry_at
    ON jobs (status, next_retry_at);