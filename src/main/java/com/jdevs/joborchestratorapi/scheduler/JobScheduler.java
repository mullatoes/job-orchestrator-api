package com.jdevs.joborchestratorapi.scheduler;

import com.jdevs.joborchestratorapi.service.JobRecoveryService;
import com.jdevs.joborchestratorapi.service.JobWorkerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobScheduler {

    private final JobWorkerService jobWorkerService;
    private final JobRecoveryService jobRecoveryService;

    @Scheduled(fixedDelayString = "${job-worker.fixed-delay-ms}")
    public void runWorkerCycle() {
        log.debug("Starting job worker cycle.");

        jobWorkerService.processReadyJobs();

        log.debug("Finished job worker cycle.");
    }

    @Scheduled(fixedDelayString = "${job-worker.recovery-fixed-delay-ms}")
    public void runRecoveryCycle() {
        log.debug("Starting stale job recovery cycle.");

        jobRecoveryService.recoverStaleProcessingJobs();

        log.debug("Finished stale job recovery cycle.");
    }
}
