package com.jdevs.joborchestratorapi.repository;

import com.jdevs.joborchestratorapi.entity.JobEntity;
import com.jdevs.joborchestratorapi.enums.JobStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<JobEntity, Long> {

    Optional<JobEntity> findByJobId(String jobId);

    boolean existsByJobId(String jobId);

    List<JobEntity> findByStatusOrderByCreatedAtAsc(JobStatus status);

    @Query("""
            SELECT j
            FROM JobEntity j
            WHERE j.status IN :statuses
            AND (j.nextRetryAt IS NULL OR j.nextRetryAt <= :now)
            ORDER BY j.createdAt ASC
            """)
    List<JobEntity> findReadyJobs(
            Collection<JobStatus> statuses,
            LocalDateTime now,
            Pageable pageable
    );
}
