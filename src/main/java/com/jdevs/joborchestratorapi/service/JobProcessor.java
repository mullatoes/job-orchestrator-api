package com.jdevs.joborchestratorapi.service;

import com.jdevs.joborchestratorapi.entity.JobEntity;
import com.jdevs.joborchestratorapi.enums.JobType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JobProcessor {

    public void process(JobEntity job) {
        log.info("Processing job. jobType={}", job.getJobType());

        if (job.getPayload() != null && job.getPayload().contains("\"forceFail\":true")) {
            throw new IllegalStateException("Forced job failure for testing retry logic");
        }

        simulateProcessing(job);

        log.info("Job processor completed successfully. jobType={}", job.getJobType());
    }

    private void simulateProcessing(JobEntity job) {
        if (job.getJobType() == JobType.EMAIL_NOTIFICATION) {
            processEmailNotification();
            return;
        }

        if (job.getJobType() == JobType.SMS_NOTIFICATION) {
            processSmsNotification();
            return;
        }

        if (job.getJobType() == JobType.REPORT_GENERATION) {
            processReportGeneration();
            return;
        }

        if (job.getJobType() == JobType.DATA_SYNC) {
            processDataSync();
            return;
        }

        throw new IllegalArgumentException("Unsupported job type: " + job.getJobType());
    }

    private void processEmailNotification() {
        log.info("Simulating email notification job.");
    }

    private void processSmsNotification() {
        log.info("Simulating SMS notification job.");
    }

    private void processReportGeneration() {
        log.info("Simulating report generation job.");
    }

    private void processDataSync() {
        log.info("Simulating data sync job.");
    }
}
