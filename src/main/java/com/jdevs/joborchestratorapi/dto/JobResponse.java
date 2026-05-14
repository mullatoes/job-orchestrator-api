package com.jdevs.joborchestratorapi.dto;

import com.jdevs.joborchestratorapi.enums.JobStatus;
import com.jdevs.joborchestratorapi.enums.JobType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class JobResponse {

    private String jobId;
    private String correlationId;
    private JobType jobType;
    private JobStatus status;
    private int attemptCount;
    private int maxAttempts;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime nextRetryAt;
}
