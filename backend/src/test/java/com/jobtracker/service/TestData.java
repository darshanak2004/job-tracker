package com.jobtracker.service;

import com.jobtracker.entity.ApplicationStatus;
import com.jobtracker.entity.JobApplication;
import com.jobtracker.entity.JobPosting;
import com.jobtracker.entity.JobStatus;
import com.jobtracker.entity.Role;
import com.jobtracker.entity.User;

/** Small builders so each test stays short and readable. */
final class TestData {

    private TestData() {
    }

    static User user(Long id, Role role) {
        return User.builder().id(id).name("User " + id).email("user" + id + "@test.com")
                .password("hash").role(role).build();
    }

    static JobPosting job(Long id, User recruiter, JobStatus status) {
        return JobPosting.builder().id(id).title("Java Dev").company("Acme").location("Remote")
                .description("Build things").status(status).recruiter(recruiter).build();
    }

    static JobApplication application(Long id, JobPosting job, User candidate) {
        return JobApplication.builder().id(id).job(job).candidate(candidate)
                .coverLetter("Hire me").status(ApplicationStatus.APPLIED).build();
    }
}
