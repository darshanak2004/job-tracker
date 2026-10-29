package com.jobtracker.dto;

import com.jobtracker.entity.JobStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JobRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be at most 200 characters")
        String title,

        @NotBlank(message = "Company is required")
        @Size(max = 200)
        String company,

        @NotBlank(message = "Location is required")
        @Size(max = 200)
        String location,

        @NotBlank(message = "Description is required")
        @Size(max = 10000)
        String description,

        @Size(max = 100)
        String salaryRange,

        /** Optional. Defaults to OPEN on create. */
        JobStatus status) {
}
