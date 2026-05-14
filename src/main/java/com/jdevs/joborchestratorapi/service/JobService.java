package com.jdevs.joborchestratorapi.service;

import com.jdevs.joborchestratorapi.dto.*;
import com.jdevs.joborchestratorapi.exception.InvalidJobStateException;
import com.jdevs.joborchestratorapi.exception.JobNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.jdevs.joborchestratorapi.entity.JobEntity;
import com.jdevs.joborchestratorapi.enums.JobStatus;
import com.jdevs.joborchestratorapi.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    @Transactional
    public RetryJobResponse retryDeadLetterJob(String jobId) {
        JobEntity job = jobRepository.findByJobId(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));

        JobStatus previousStatus = job.getStatus();

        if (previousStatus != JobStatus.DEAD_LETTER) {
            throw new InvalidJobStateException(
                    "Only DEAD_LETTER jobs can be manually retried. jobId=" + jobId + ", currentStatus=" + previousStatus
            );
        }

        job.setStatus(JobStatus.RETRYABLE);
        job.setAttemptCount(0);
        job.setErrorMessage(null);
        job.setNextRetryAt(LocalDateTime.now());
        job.setLockedBy(null);
        job.setLockedAt(null);
        job.setStartedAt(null);
        job.setCompletedAt(null);

        return RetryJobResponse.builder()
                .jobId(job.getJobId())
                .previousStatus(previousStatus)
                .currentStatus(job.getStatus())
                .attemptCount(job.getAttemptCount())
                .build();
    }

    @Transactional(readOnly = true)
    public PagedResponse<JobResponse> searchJobs(JobStatus status, int page, int size) {
        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<JobEntity> jobPage;

        if (status == null) {
            jobPage = jobRepository.findAll(pageRequest);
        } else {
            jobPage = jobRepository.findByStatus(status, pageRequest);
        }

        List<JobResponse> jobs = jobPage.getContent()
                .stream()
                .map(this::toJobResponse)
                .toList();

        return PagedResponse.<JobResponse>builder()
                .content(jobs)
                .page(jobPage.getNumber())
                .size(jobPage.getSize())
                .totalElements(jobPage.getTotalElements())
                .totalPages(jobPage.getTotalPages())
                .first(jobPage.isFirst())
                .last(jobPage.isLast())
                .empty(jobPage.isEmpty())
                .build();
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
