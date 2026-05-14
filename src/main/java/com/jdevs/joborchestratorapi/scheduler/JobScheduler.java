package com.jdevs.joborchestratorapi.scheduler;

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

    @Scheduled(fixedDelayString = "${job-worker.fixed-delay-ms}")
    public void runWorkerCycle() {
        log.debug("Starting job worker cycle.");

        jobWorkerService.processReadyJobs();

        log.debug("Finished job worker cycle.");
    }
}
