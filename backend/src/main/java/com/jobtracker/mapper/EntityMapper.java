package com.jobtracker.mapper;

import com.jobtracker.dto.ApplicationResponse;
import com.jobtracker.dto.JobResponse;
import com.jobtracker.dto.UserResponse;
import com.jobtracker.entity.JobApplication;
import com.jobtracker.entity.JobPosting;
import com.jobtracker.entity.User;

/** Entity -> DTO conversion. Entities never leave the service layer. */
public final class EntityMapper {

    private EntityMapper() {
    }

    public static UserResponse toResponse(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole());
    }

    public static JobResponse toResponse(JobPosting j) {
        return new JobResponse(j.getId(), j.getTitle(), j.getCompany(), j.getLocation(),
                j.getDescription(), j.getSalaryRange(), j.getStatus(),
                j.getRecruiter().getId(), j.getRecruiter().getName(), j.getCreatedAt());
    }

    public static ApplicationResponse toResponse(JobApplication a) {
        return new ApplicationResponse(a.getId(), a.getJob().getId(), a.getJob().getTitle(),
                a.getJob().getCompany(), a.getCandidate().getId(), a.getCandidate().getName(),
                a.getCandidate().getEmail(), a.getCoverLetter(), a.getStatus(), a.getAppliedAt());
    }
}
