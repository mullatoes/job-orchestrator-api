package com.jdevs.joborchestratorapi.dto;

import com.jdevs.joborchestratorapi.enums.JobStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RetryJobResponse {

    private String jobId;
    private JobStatus previousStatus;
    private JobStatus currentStatus;
    private int attemptCount;
}
