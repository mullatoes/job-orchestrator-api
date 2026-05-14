package com.jdevs.joborchestratorapi.repository;

import com.jdevs.joborchestratorapi.entity.JobEntity;
import com.jdevs.joborchestratorapi.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<JobEntity, Long> {

    Optional<JobEntity> findByJobId(String jobId);

    boolean existsByJobId(String jobId);

    List<JobEntity> findByStatusOrderByCreatedAtAsc(JobStatus status);
}
