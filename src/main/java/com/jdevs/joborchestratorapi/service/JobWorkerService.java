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

    private final JobClaimService jobClaimService;
    private final JobExecutionService jobExecutionService;
    private final JobWorkerProperties properties;

    public void processReadyJobs() {
        if (!properties.isEnabled()) {
            log.debug("Job worker is disabled. Skipping processing cycle.");
            return;
        }

        List<Long> claimedJobIds = jobClaimService.claimReadyJobs();

        if (claimedJobIds.isEmpty()) {
            return;
        }

        for (Long jobDatabaseId : claimedJobIds) {
            try {
                jobExecutionService.processJob(jobDatabaseId);
            } catch (Exception ex) {
                log.error(
                        "Unexpected error while executing claimed job. jobDatabaseId={}",
                        jobDatabaseId,
                        ex
                );
            }
        }
    }
}
