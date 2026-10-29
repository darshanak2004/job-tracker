package com.jobtracker.controller;

import com.jobtracker.dto.ApplicationResponse;
import com.jobtracker.dto.ApplyRequest;
import com.jobtracker.dto.StatusUpdateRequest;
import com.jobtracker.security.AuthUser;
import com.jobtracker.service.ApplicationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    /** Candidate applies to a job. */
    @PostMapping("/api/jobs/{jobId}/applications")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CANDIDATE')")
    public ApplicationResponse apply(@AuthenticationPrincipal AuthUser user, @PathVariable Long jobId,
                                     @Valid @RequestBody(required = false) ApplyRequest request) {
        return applicationService.apply(user.getId(), jobId, request);
    }

    /** Recruiter lists applicants for one of their jobs. */
    @GetMapping("/api/jobs/{jobId}/applications")
    @PreAuthorize("hasRole('RECRUITER')")
    public List<ApplicationResponse> forJob(@AuthenticationPrincipal AuthUser user, @PathVariable Long jobId) {
        return applicationService.listForJob(user.getId(), jobId);
    }

    /** Candidate lists their own applications. */
    @GetMapping("/api/applications/mine")
    @PreAuthorize("hasRole('CANDIDATE')")
    public List<ApplicationResponse> mine(@AuthenticationPrincipal AuthUser user) {
        return applicationService.listForCandidate(user.getId());
    }

    /** Recruiter moves an application through the pipeline. */
    @PatchMapping("/api/applications/{id}/status")
    @PreAuthorize("hasRole('RECRUITER')")
    public ApplicationResponse updateStatus(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                            @Valid @RequestBody StatusUpdateRequest request) {
        return applicationService.updateStatus(user.getId(), id, request.status());
    }

    /** Candidate withdraws their application. */
    @DeleteMapping("/api/applications/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('CANDIDATE')")
    public void withdraw(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        applicationService.withdraw(user.getId(), id);
    }
}
