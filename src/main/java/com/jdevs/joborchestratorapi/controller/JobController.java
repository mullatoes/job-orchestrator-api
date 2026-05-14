package com.jdevs.joborchestratorapi.controller;

import com.jdevs.joborchestratorapi.dto.*;
import com.jdevs.joborchestratorapi.enums.JobStatus;
import com.jdevs.joborchestratorapi.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @PostMapping
    public ResponseEntity<ApiResponse<SubmitJobResponse>> submitJob(
            @Valid @RequestBody SubmitJobRequest request
    ) {
        log.info("Received submit job request. jobType={}, maxAttempts={}",
                request.getJobType(),
                request.getMaxAttempts()
        );

        SubmitJobResponse response = jobService.submitJob(request);

        log.info("Submit job request completed. jobId={}, status={}",
                response.getJobId(),
                response.getStatus()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Job submitted successfully", response));
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<ApiResponse<JobResponse>> getJobById(
            @PathVariable String jobId
    ) {
        log.info("Received get job request. jobId={}", jobId);

        JobResponse response = jobService.getJobByJobId(jobId);

        log.info("Get job request completed. jobId={}, status={}",
                response.getJobId(),
                response.getStatus()
        );

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

        log.info("Received search jobs request. status={}, page={}, size={}", status, page, size);

        PagedResponse<JobResponse> response = jobService.searchJobs(status, page, size);

        log.info("Search jobs request completed. status={}, page={}, size={}, totalElements={}",
                status,
                response.getPage(),
                response.getSize(),
                response.getTotalElements()
        );

        return ResponseEntity.ok(
                ApiResponse.success("Jobs retrieved successfully", response)
        );
    }

    /**
     * Manual job retry
     * @param jobId
     * @return
     */
    @PostMapping("/{jobId}/retry")
    public ResponseEntity<ApiResponse<RetryJobResponse>> retryDeadLetterJob(
            @PathVariable String jobId
    ) {
        log.info("Received manual retry request. jobId={}", jobId);

        RetryJobResponse response = jobService.retryDeadLetterJob(jobId);

        log.info("Manual retry request completed. jobId={}, previousStatus={}, currentStatus={}",
                response.getJobId(),
                response.getPreviousStatus(),
                response.getCurrentStatus()
        );

        return ResponseEntity.ok(
                ApiResponse.success("Job requeued successfully", response)
        );
    }
}
