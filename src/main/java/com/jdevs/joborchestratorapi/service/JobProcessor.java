package com.jdevs.joborchestratorapi.service;

import com.jdevs.joborchestratorapi.entity.JobEntity;
import com.jdevs.joborchestratorapi.enums.JobType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JobProcessor {

    public void process(JobEntity job) {
        log.info(
                "Processing job. jobId={}, jobType={}, attemptCount={}",
                job.getJobId(),
                job.getJobType(),
                job.getAttemptCount()
        );

        if (job.getPayload() != null && job.getPayload().contains("\"forceFail\":true")) {
            throw new IllegalStateException("Forced job failure for testing retry logic");
        }

        simulateProcessing(job);

        log.info(
                "Job processing completed. jobId={}, jobType={}",
                job.getJobId(),
                job.getJobType()
        );
    }

    private void simulateProcessing(JobEntity job) {
        if (job.getJobType() == JobType.EMAIL_NOTIFICATION) {
            processEmailNotification(job);
            return;
        }

        if (job.getJobType() == JobType.SMS_NOTIFICATION) {
            processSmsNotification(job);
            return;
        }

        if (job.getJobType() == JobType.REPORT_GENERATION) {
            processReportGeneration(job);
            return;
        }

        if (job.getJobType() == JobType.DATA_SYNC) {
            processDataSync(job);
            return;
        }

        throw new IllegalArgumentException("Unsupported job type: " + job.getJobType());
    }

    private void processEmailNotification(JobEntity job) {
        log.info("Simulating email notification job. jobId={}", job.getJobId());
    }

    private void processSmsNotification(JobEntity job) {
        log.info("Simulating SMS notification job. jobId={}", job.getJobId());
    }

    private void processReportGeneration(JobEntity job) {
        log.info("Simulating report generation job. jobId={}", job.getJobId());
    }

    private void processDataSync(JobEntity job) {
        log.info("Simulating data sync job. jobId={}", job.getJobId());
    }
}
