package com.jdevs.joborchestratorapi.service;


import com.jdevs.joborchestratorapi.config.JobWorkerProperties;
import com.jdevs.joborchestratorapi.entity.JobEntity;
import com.jdevs.joborchestratorapi.enums.JobStatus;
import com.jdevs.joborchestratorapi.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobRecoveryService {

    private static final String JOB_ID = "jobId";
    private static final String WORKER_ID = "workerId";

    private final JobRepository jobRepository;
    private final JobWorkerProperties properties;

    @Transactional
    public void recoverStaleProcessingJobs() {
        try {
            MDC.put(WORKER_ID, properties.getWorkerId());

            if (!properties.isEnabled()) {
                log.debug("Job recovery is disabled because worker is disabled.");
                return;
            }

            LocalDateTime staleBefore = LocalDateTime.now()
                    .minusSeconds(properties.getStaleTimeoutSeconds());

            List<JobEntity> staleJobs = jobRepository.findStaleProcessingJobs(
                    JobStatus.PROCESSING,
                    staleBefore,
                    PageRequest.of(0, properties.getBatchSize())
            );

            if (staleJobs.isEmpty()) {
                log.debug("No stale PROCESSING jobs found.");
                return;
            }

            log.warn("Found stale PROCESSING jobs. count={}, staleBefore={}", staleJobs.size(), staleBefore);

            for (JobEntity job : staleJobs) {
                recoverSingleJob(job);
            }
        } finally {
            MDC.remove(WORKER_ID);
        }
    }
    private void recoverSingleJob(JobEntity job) {
        try {
            MDC.put(JOB_ID, job.getJobId());

            int nextAttemptCount = job.getAttemptCount() + 1;
            job.setAttemptCount(nextAttemptCount);

            String recoveryMessage = "Job recovered from stale PROCESSING state";

            job.setErrorMessage(recoveryMessage);

            if (nextAttemptCount >= job.getMaxAttempts()) {
                job.setStatus(JobStatus.DEAD_LETTER);
                job.setNextRetryAt(null);

                log.warn(
                        "Stale job moved to DEAD_LETTER. attemptCount={}, maxAttempts={}, lockedBy={}, lockedAt={}",
                        job.getAttemptCount(),
                        job.getMaxAttempts(),
                        job.getLockedBy(),
                        job.getLockedAt()
                );
                return;
            }

            LocalDateTime nextRetryAt = LocalDateTime.now()
                    .plusSeconds(properties.getRetryDelaySeconds());

            job.setStatus(JobStatus.RETRYABLE);
            job.setNextRetryAt(nextRetryAt);

            log.warn(
                    "Stale job recovered as RETRYABLE. attemptCount={}, maxAttempts={}, nextRetryAt={}, lockedBy={}, lockedAt={}",
                    job.getAttemptCount(),
                    job.getMaxAttempts(),
                    nextRetryAt,
                    job.getLockedBy(),
                    job.getLockedAt()
            );
        } finally {
            MDC.remove(JOB_ID);
        }
    }
}
