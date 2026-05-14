package com.jdevs.joborchestratorapi.service;

import com.jdevs.joborchestratorapi.config.JobWorkerProperties;
import com.jdevs.joborchestratorapi.entity.JobEntity;
import com.jdevs.joborchestratorapi.enums.JobStatus;
import com.jdevs.joborchestratorapi.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobExecutionService {

    private final JobRepository jobRepository;
    private final JobProcessor jobProcessor;
    private final JobWorkerProperties properties;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processJob(Long jobDatabaseId) {
        JobEntity job = jobRepository.findById(jobDatabaseId)
                .orElseThrow(() -> new IllegalStateException("Job not found with id: " + jobDatabaseId));

        if (!isProcessable(job)) {
            log.info(
                    "Skipping job because it is no longer processable. jobId={}, status={}",
                    job.getJobId(),
                    job.getStatus()
            );
            return;
        }

        try {
            markAsProcessing(job);

            jobProcessor.process(job);

            markAsCompleted(job);
        } catch (Exception ex) {
            handleProcessingFailure(job, ex);
        }
    }

    private boolean isProcessable(JobEntity job) {
        return job.getStatus() == JobStatus.PENDING
                || job.getStatus() == JobStatus.RETRYABLE;
    }

    private void markAsProcessing(JobEntity job) {
        LocalDateTime now = LocalDateTime.now();

        job.setStatus(JobStatus.PROCESSING);
        job.setStartedAt(now);
        job.setLockedAt(now);
        job.setLockedBy(properties.getWorkerId());
        job.setErrorMessage(null);

        log.info(
                "Job marked as PROCESSING. jobId={}, workerId={}",
                job.getJobId(),
                properties.getWorkerId()
        );
    }

    private void markAsCompleted(JobEntity job) {
        job.setStatus(JobStatus.COMPLETED);
        job.setCompletedAt(LocalDateTime.now());
        job.setNextRetryAt(null);
        job.setErrorMessage(null);

        log.info("Job marked as COMPLETED. jobId={}", job.getJobId());
    }

    private void handleProcessingFailure(JobEntity job, Exception ex) {
        int nextAttemptCount = job.getAttemptCount() + 1;
        job.setAttemptCount(nextAttemptCount);

        String safeErrorMessage = ex.getMessage() == null
                ? "Job processing failed"
                : ex.getMessage();

        job.setErrorMessage(safeErrorMessage);

        if (nextAttemptCount >= job.getMaxAttempts()) {
            job.setStatus(JobStatus.DEAD_LETTER);
            job.setNextRetryAt(null);

            log.warn(
                    "Job moved to DEAD_LETTER. jobId={}, attemptCount={}, maxAttempts={}, reason={}",
                    job.getJobId(),
                    job.getAttemptCount(),
                    job.getMaxAttempts(),
                    safeErrorMessage
            );
            return;
        }

        LocalDateTime nextRetryAt = LocalDateTime.now()
                .plusSeconds(properties.getRetryDelaySeconds());

        job.setStatus(JobStatus.RETRYABLE);
        job.setNextRetryAt(nextRetryAt);

        log.warn(
                "Job marked as RETRYABLE. jobId={}, attemptCount={}, maxAttempts={}, nextRetryAt={}, reason={}",
                job.getJobId(),
                job.getAttemptCount(),
                job.getMaxAttempts(),
                nextRetryAt,
                safeErrorMessage
        );
    }
}
