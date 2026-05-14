package com.jdevs.joborchestratorapi.repository;

import com.jdevs.joborchestratorapi.entity.JobEntity;
import com.jdevs.joborchestratorapi.enums.JobStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
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

    @Query("""
        SELECT j
        FROM JobEntity j
        WHERE j.status = :status
        AND j.lockedAt IS NOT NULL
        AND j.lockedAt <= :staleBefore
        ORDER BY j.lockedAt ASC
        """)
    List<JobEntity> findStaleProcessingJobs(
            JobStatus status,
            LocalDateTime staleBefore,
            Pageable pageable
    );
}
