package com.jdevs.joborchestratorapi.repository;

import com.jdevs.joborchestratorapi.entity.JobEntity;
import com.jdevs.joborchestratorapi.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<JobEntity, Long> {

    Optional<JobEntity> findByJobId(String jobId);

    boolean existsByJobId(String jobId);

    List<JobEntity> findByStatusOrderByCreatedAtAsc(JobStatus status);

    @Query(
            value = """
                    SELECT *
                    FROM jobs
                    WHERE status IN ('PENDING', 'RETRYABLE')
                    AND (next_retry_at IS NULL OR next_retry_at <= CURRENT_TIMESTAMP)
                    ORDER BY created_at ASC
                    FOR UPDATE SKIP LOCKED
                    LIMIT :batchSize
                    """,
            nativeQuery = true
    )
    List<JobEntity> findReadyJobsForUpdate(int batchSize);
}
