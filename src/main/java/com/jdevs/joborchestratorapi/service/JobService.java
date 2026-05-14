package com.jdevs.joborchestratorapi.service;

import com.jdevs.joborchestratorapi.exception.JobNotFoundException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.jdevs.joborchestratorapi.dto.JobResponse;
import com.jdevs.joborchestratorapi.dto.SubmitJobRequest;
import com.jdevs.joborchestratorapi.dto.SubmitJobResponse;
import com.jdevs.joborchestratorapi.entity.JobEntity;
import com.jdevs.joborchestratorapi.enums.JobStatus;
import com.jdevs.joborchestratorapi.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public SubmitJobResponse submitJob(SubmitJobRequest request) {
        String jobId = generateJobId();

        JobEntity job = JobEntity.builder()
                .jobId(jobId)
                .jobType(request.getJobType())
                .status(JobStatus.PENDING)
                .payload(toJson(request.getPayload()))
                .attemptCount(0)
                .maxAttempts(request.getMaxAttempts() == null ? 3 : request.getMaxAttempts())
                .build();

        JobEntity savedJob = jobRepository.save(job);

        return SubmitJobResponse.builder()
                .jobId(savedJob.getJobId())
                .jobType(savedJob.getJobType())
                .status(savedJob.getStatus())
                .build();
    }

    @Transactional(readOnly = true)
    public JobResponse getJobByJobId(String jobId) {
        JobEntity job = jobRepository.findByJobId(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));

        return toJobResponse(job);
    }

    @Transactional(readOnly = true)
    public List<JobResponse> getJobsByStatus(JobStatus status) {
        return jobRepository.findByStatusOrderByCreatedAtAsc(status)
                .stream()
                .map(this::toJobResponse)
                .toList();
    }

    private JobResponse toJobResponse(JobEntity job) {
        return JobResponse.builder()
                .jobId(job.getJobId())
                .jobType(job.getJobType())
                .status(job.getStatus())
                .attemptCount(job.getAttemptCount())
                .maxAttempts(job.getMaxAttempts())
                .errorMessage(job.getErrorMessage())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .startedAt(job.getStartedAt())
                .completedAt(job.getCompletedAt())
                .nextRetryAt(job.getNextRetryAt())
                .build();
    }

    private String generateJobId() {
        return "JOB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException ex) {
            throw new IllegalArgumentException("Invalid job payload");
        }
    }
}
