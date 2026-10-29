package com.jobtracker.controller;

import com.jobtracker.dto.JobRequest;
import com.jobtracker.dto.JobResponse;
import com.jobtracker.dto.PageResponse;
import com.jobtracker.security.AuthUser;
import com.jobtracker.service.JobPostingService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs")
public class JobPostingController {

    private final JobPostingService jobService;

    public JobPostingController(JobPostingService jobService) {
        this.jobService = jobService;
    }

    /** Any logged-in user can browse open jobs. */
    @GetMapping
    public PageResponse<JobResponse> search(@RequestParam(required = false) String keyword,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        return jobService.searchOpenJobs(keyword, page, size);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('RECRUITER')")
    public List<JobResponse> mine(@AuthenticationPrincipal AuthUser user) {
        return jobService.listByRecruiter(user.getId());
    }

    @GetMapping("/{id}")
    public JobResponse get(@PathVariable Long id) {
        return jobService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('RECRUITER')")
    public JobResponse create(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody JobRequest request) {
        return jobService.create(user.getId(), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('RECRUITER')")
    public JobResponse update(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                              @Valid @RequestBody JobRequest request) {
        return jobService.update(user.getId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('RECRUITER')")
    public void delete(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        jobService.delete(user.getId(), id);
    }
}
