package com.jdevs.joborchestratorapi.dto;

import com.jdevs.joborchestratorapi.enums.JobType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class SubmitJobRequest {

    @NotNull(message = "jobType is required")
    private JobType jobType;

    @NotNull(message = "payload is required")
    private Map<String, Object> payload;

    @Min(value = 1, message = "maxAttempts must be at least 1")
    @Max(value = 10, message = "maxAttempts cannot be more than 10")
    private Integer maxAttempts;
}
