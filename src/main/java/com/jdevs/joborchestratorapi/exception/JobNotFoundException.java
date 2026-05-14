package com.jdevs.joborchestratorapi.exception;

public class JobNotFoundException extends RuntimeException {

    public JobNotFoundException(String jobId) {
        super("Job not found with jobId: " + jobId);
    }
}
