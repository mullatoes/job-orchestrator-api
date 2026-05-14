package com.jdevs.joborchestratorapi.service;

import com.jdevs.joborchestratorapi.config.JobWorkerProperties;
import com.jdevs.joborchestratorapi.entity.JobEntity;
import com.jdevs.joborchestratorapi.enums.JobStatus;
import com.jdevs.joborchestratorapi.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobWorkerService {

    private final JobRepository jobRepository;
    private final JobExecutionService jobExecutionService;
    private final JobWorkerProperties properties;

    public void processReadyJobs() {
        if (!properties.isEnabled()) {
            log.debug("Job worker is disabled. Skipping processing cycle.");
            return;
        }

        List<JobEntity> readyJobs = jobRepository.findReadyJobs(
                List.of(JobStatus.PENDING, JobStatus.RETRYABLE),
                LocalDateTime.now(),
                PageRequest.of(0, properties.getBatchSize())
        );

        if (readyJobs.isEmpty()) {
            log.debug("No ready jobs found for processing.");
            return;
        }

        log.info("Found ready jobs for processing. count={}", readyJobs.size());

        for (JobEntity job : readyJobs) {
            try {
                jobExecutionService.processJob(job.getId());
            } catch (Exception ex) {
                log.error(
                        "Unexpected error while processing job. jobId={}",
                        job.getJobId(),
                        ex
                );
            }
        }
    }
}
