package com.jobtracker.dto;

import com.jobtracker.entity.JobStatus;
import java.time.Instant;

public record JobResponse(
        Long id,
        String title,
        String company,
        String location,
        String description,
        String salaryRange,
        JobStatus status,
        Long recruiterId,
        String recruiterName,
        Instant createdAt) {
}
