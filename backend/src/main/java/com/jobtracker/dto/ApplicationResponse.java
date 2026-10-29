package com.jobtracker.dto;

import com.jobtracker.entity.ApplicationStatus;
import java.time.Instant;

public record ApplicationResponse(
        Long id,
        Long jobId,
        String jobTitle,
        String company,
        Long candidateId,
        String candidateName,
        String candidateEmail,
        String coverLetter,
        ApplicationStatus status,
        Instant appliedAt) {
}
