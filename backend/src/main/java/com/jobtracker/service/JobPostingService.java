package com.jobtracker.service;

import com.jobtracker.dto.JobRequest;
import com.jobtracker.dto.JobResponse;
import com.jobtracker.dto.PageResponse;
import com.jobtracker.entity.JobPosting;
import com.jobtracker.entity.JobStatus;
import com.jobtracker.entity.User;
import com.jobtracker.exception.ForbiddenException;
import com.jobtracker.exception.ResourceNotFoundException;
import com.jobtracker.mapper.EntityMapper;
import com.jobtracker.repository.JobPostingRepository;
import com.jobtracker.repository.UserRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobPostingService {

    private final JobPostingRepository jobRepository;
    private final UserRepository userRepository;

    public JobPostingService(JobPostingRepository jobRepository, UserRepository userRepository) {
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public JobResponse create(Long recruiterId, JobRequest req) {
        User recruiter = userRepository.findById(recruiterId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        JobPosting job = JobPosting.builder()
                .title(req.title().trim())
                .company(req.company().trim())
                .location(req.location().trim())
                .description(req.description().trim())
                .salaryRange(req.salaryRange())
                .status(req.status() == null ? JobStatus.OPEN : req.status())
                .recruiter(recruiter)
                .build();
        return EntityMapper.toResponse(jobRepository.save(job));
    }

    @Transactional(readOnly = true)
    public JobResponse getById(Long id) {
        return EntityMapper.toResponse(find(id));
    }

    /** Public listing for candidates: only OPEN jobs, optional keyword on title/company/location. */
    @Transactional(readOnly = true)
    public PageResponse<JobResponse> searchOpenJobs(String keyword, int page, int size) {
        String pattern = "%" + (keyword == null ? "" : keyword.trim().toLowerCase()) + "%";
        int safeSize = Math.min(Math.max(size, 1), 50);
        var result = jobRepository.search(JobStatus.OPEN, pattern, PageRequest.of(Math.max(page, 0), safeSize))
                .map(EntityMapper::toResponse);
        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public List<JobResponse> listByRecruiter(Long recruiterId) {
        return jobRepository.findByRecruiterIdOrderByCreatedAtDesc(recruiterId).stream()
                .map(EntityMapper::toResponse)
                .toList();
    }

    @Transactional
    public JobResponse update(Long recruiterId, Long jobId, JobRequest req) {
        JobPosting job = find(jobId);
        assertOwner(job, recruiterId); // ownership check
        job.setTitle(req.title().trim());
        job.setCompany(req.company().trim());
        job.setLocation(req.location().trim());
        job.setDescription(req.description().trim());
        job.setSalaryRange(req.salaryRange());
        if (req.status() != null) {
            job.setStatus(req.status());
        }
        return EntityMapper.toResponse(job); // dirty checking saves it on commit
    }

    @Transactional
    public void delete(Long recruiterId, Long jobId) {
        JobPosting job = find(jobId);
        assertOwner(job, recruiterId);
        jobRepository.delete(job);
    }

    private JobPosting find(Long id) {
        return jobRepository.findWithRecruiterById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job posting " + id + " not found"));
    }

    private void assertOwner(JobPosting job, Long recruiterId) {
        if (!job.getRecruiter().getId().equals(recruiterId)) {
            throw new ForbiddenException("You can only modify your own job postings");
        }
    }
}
