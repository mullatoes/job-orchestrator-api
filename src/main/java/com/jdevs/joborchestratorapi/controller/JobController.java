package com.jdevs.joborchestratorapi.controller;

import com.jdevs.joborchestratorapi.dto.ApiResponse;
import com.jdevs.joborchestratorapi.dto.JobResponse;
import com.jdevs.joborchestratorapi.dto.SubmitJobRequest;
import com.jdevs.joborchestratorapi.dto.SubmitJobResponse;
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
    public ResponseEntity<ApiResponse<List<JobResponse>>> getJobsByStatus(
            @RequestParam JobStatus status
    ) {
        List<JobResponse> response = jobService.getJobsByStatus(status);

        return ResponseEntity.ok(
                ApiResponse.success("Jobs retrieved successfully", response)
        );
    }
}
