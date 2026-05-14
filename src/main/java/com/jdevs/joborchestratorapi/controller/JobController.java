package com.jdevs.joborchestratorapi.controller;

import com.jdevs.joborchestratorapi.dto.*;
import com.jdevs.joborchestratorapi.enums.JobStatus;
import com.jdevs.joborchestratorapi.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @PostMapping
    public ResponseEntity<ApiResponse<SubmitJobResponse>> submitJob(
            @Valid @RequestBody SubmitJobRequest request
    ) {
        SubmitJobResponse response = jobService.submitJob(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Job submitted successfully", response));
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<ApiResponse<JobResponse>> getJobById(
            @PathVariable String jobId
    ) {
        JobResponse response = jobService.getJobByJobId(jobId);

        return ResponseEntity.ok(
                ApiResponse.success("Job retrieved successfully", response)
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<JobResponse>>> searchJobs(
            @RequestParam(required = false) JobStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be greater than or equal to 0");
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }

        PagedResponse<JobResponse> response = jobService.searchJobs(status, page, size);

        return ResponseEntity.ok(
                ApiResponse.success("Jobs retrieved successfully", response)
        );
    }

    @PostMapping("/{jobId}/retry")
    public ResponseEntity<ApiResponse<RetryJobResponse>> retryDeadLetterJob(
            @PathVariable String jobId
    ) {
        RetryJobResponse response = jobService.retryDeadLetterJob(jobId);

        return ResponseEntity.ok(
                ApiResponse.success("Job requeued successfully", response)
        );
    }
}
