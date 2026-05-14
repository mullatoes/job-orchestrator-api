package com.jdevs.joborchestratorapi.dto;

import com.jdevs.joborchestratorapi.enums.JobStatus;
import com.jdevs.joborchestratorapi.enums.JobType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SubmitJobResponse {

    private String jobId;
    private String correlationId;
    private JobType jobType;
    private JobStatus status;
}
