package com.jobtracker.service;

import com.jobtracker.dto.ApplicationResponse;
import com.jobtracker.dto.ApplyRequest;
import com.jobtracker.entity.ApplicationStatus;
import com.jobtracker.entity.JobApplication;
import com.jobtracker.entity.JobPosting;
import com.jobtracker.entity.JobStatus;
import com.jobtracker.entity.User;
import com.jobtracker.exception.BusinessRuleException;
import com.jobtracker.exception.DuplicateResourceException;
import com.jobtracker.exception.ForbiddenException;
import com.jobtracker.exception.ResourceNotFoundException;
import com.jobtracker.mapper.EntityMapper;
import com.jobtracker.repository.JobApplicationRepository;
import com.jobtracker.repository.JobPostingRepository;
import com.jobtracker.repository.UserRepository;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationService {

    private final JobApplicationRepository applicationRepository;
    private final JobPostingRepository jobRepository;
    private final UserRepository userRepository;

    public ApplicationService(JobApplicationRepository applicationRepository,
                              JobPostingRepository jobRepository, UserRepository userRepository) {
        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ApplicationResponse apply(Long candidateId, Long jobId, ApplyRequest req) {
        JobPosting job = jobRepository.findWithRecruiterById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job posting " + jobId + " not found"));
        if (job.getStatus() != JobStatus.OPEN) {
            throw new BusinessRuleException("This job is closed and no longer accepts applications");
        }
        // Friendly check first...
        if (applicationRepository.existsByJobIdAndCandidateId(jobId, candidateId)) {
            throw new DuplicateResourceException("You have already applied to this job");
        }
        User candidate = userRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        JobApplication application = JobApplication.builder()
                .job(job)
                .candidate(candidate)
                .coverLetter(req == null ? null : req.coverLetter())
                .status(ApplicationStatus.APPLIED)
                .build();
        try {
            // ...and the DB unique constraint is the safety net if two requests race each other.
            return EntityMapper.toResponse(applicationRepository.saveAndFlush(application));
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateResourceException("You have already applied to this job");
        }
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> listForCandidate(Long candidateId) {
        return applicationRepository.findByCandidateIdOrderByAppliedAtDesc(candidateId).stream()
                .map(EntityMapper::toResponse)
                .toList();
    }

    /** Recruiter sees applicants - but only for jobs they own. */
    @Transactional(readOnly = true)
    public List<ApplicationResponse> listForJob(Long recruiterId, Long jobId) {
        JobPosting job = jobRepository.findWithRecruiterById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job posting " + jobId + " not found"));
        if (!job.getRecruiter().getId().equals(recruiterId)) {
            throw new ForbiddenException("You can only view applications for your own job postings");
        }
        return applicationRepository.findByJobIdOrderByAppliedAtDesc(jobId).stream()
                .map(EntityMapper::toResponse)
                .toList();
    }

    @Transactional
    public ApplicationResponse updateStatus(Long recruiterId, Long applicationId, ApplicationStatus newStatus) {
        JobApplication application = findWithDetails(applicationId);
        if (!application.getJob().getRecruiter().getId().equals(recruiterId)) {
            throw new ForbiddenException("You can only update applications for your own job postings");
        }
        application.setStatus(newStatus);
        return EntityMapper.toResponse(application);
    }

    /** Candidate withdraws (deletes) their own application. */
    @Transactional
    public void withdraw(Long candidateId, Long applicationId) {
        JobApplication application = findWithDetails(applicationId);
        if (!application.getCandidate().getId().equals(candidateId)) {
            throw new ForbiddenException("You can only withdraw your own applications");
        }
        applicationRepository.delete(application);
    }

    private JobApplication findWithDetails(Long id) {
        return applicationRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application " + id + " not found"));
    }
}
