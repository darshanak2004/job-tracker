package com.jobtracker.dto;

import jakarta.validation.constraints.Size;

public record ApplyRequest(
        @Size(max = 5000, message = "Cover letter must be at most 5000 characters")
        String coverLetter) {
}
