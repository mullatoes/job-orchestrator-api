package com.jdevs.joborchestratorapi.service;

import com.jdevs.joborchestratorapi.config.JobWorkerProperties;
import com.jdevs.joborchestratorapi.entity.JobEntity;
import com.jdevs.joborchestratorapi.enums.JobStatus;
import com.jdevs.joborchestratorapi.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobExecutionService {

    private static final String JOB_ID = "jobId";
    private static final String WORKER_ID = "workerId";

    private final JobRepository jobRepository;
    private final JobProcessor jobProcessor;
    private final JobWorkerProperties properties;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processJob(Long jobDatabaseId) {
        JobEntity job = jobRepository.findById(jobDatabaseId)
                .orElseThrow(() -> new IllegalStateException("Job not found with id: " + jobDatabaseId));

        try {
            MDC.put(JOB_ID, job.getJobId());
            MDC.put(WORKER_ID, properties.getWorkerId());

            if (!isProcessable(job)) {
                log.info(
                        "Skipping job because it is no longer processable. status={}",
                        job.getStatus()
                );
                return;
            }

            log.info(
                    "Starting job execution. jobType={}, attemptCount={}, maxAttempts={}",
                    job.getJobType(),
                    job.getAttemptCount(),
                    job.getMaxAttempts()
            );

            jobProcessor.process(job);

            markAsCompleted(job);

            log.info("Finished job execution successfully.");
        } catch (Exception ex) {
            handleProcessingFailure(job, ex);
        } finally {
            MDC.remove(JOB_ID);
            MDC.remove(WORKER_ID);
        }
    }
    private boolean isProcessable(JobEntity job) {
        return job.getStatus() == JobStatus.PROCESSING;
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
