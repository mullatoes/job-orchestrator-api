package com.jdevs.joborchestratorapi.service;

import com.jdevs.joborchestratorapi.config.JobWorkerProperties;
import com.jdevs.joborchestratorapi.entity.JobEntity;
import com.jdevs.joborchestratorapi.enums.JobStatus;
import com.jdevs.joborchestratorapi.logging.MdcKeys;
import com.jdevs.joborchestratorapi.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobClaimService {

    private final JobRepository jobRepository;
    private final JobWorkerProperties properties;

    @Transactional
    public List<Long> claimReadyJobs() {
        try {
            MDC.put(MdcKeys.WORKER_ID, properties.getWorkerId());

            List<JobEntity> readyJobs = jobRepository.findReadyJobsForUpdate(properties.getBatchSize());

            if (readyJobs.isEmpty()) {
                log.debug("No jobs available for claiming.");
                return List.of();
            }

            LocalDateTime now = LocalDateTime.now();

            for (JobEntity job : readyJobs) {
                job.setStatus(JobStatus.PROCESSING);
                job.setStartedAt(now);
                job.setLockedAt(now);
                job.setLockedBy(properties.getWorkerId());
                job.setErrorMessage(null);
            }

            List<Long> claimedJobIds = readyJobs.stream()
                    .map(JobEntity::getId)
                    .toList();

            log.info("Claimed jobs for processing. count={}", claimedJobIds.size());

            return claimedJobIds;
        } finally {
            MDC.remove(MdcKeys.WORKER_ID);
        }
    }
}
