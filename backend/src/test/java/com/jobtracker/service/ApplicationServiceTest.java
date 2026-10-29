package com.jobtracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jobtracker.dto.ApplicationResponse;
import com.jobtracker.dto.ApplyRequest;
import com.jobtracker.entity.ApplicationStatus;
import com.jobtracker.entity.JobApplication;
import com.jobtracker.entity.JobPosting;
import com.jobtracker.entity.JobStatus;
import com.jobtracker.entity.Role;
import com.jobtracker.entity.User;
import com.jobtracker.exception.BusinessRuleException;
import com.jobtracker.exception.DuplicateResourceException;
import com.jobtracker.exception.ForbiddenException;
import com.jobtracker.exception.ResourceNotFoundException;
import com.jobtracker.repository.JobApplicationRepository;
import com.jobtracker.repository.JobPostingRepository;
import com.jobtracker.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock JobApplicationRepository applicationRepository;
    @Mock JobPostingRepository jobRepository;
    @Mock UserRepository userRepository;
    @InjectMocks ApplicationService service;

    User recruiter;
    User candidate;
    JobPosting openJob;

    @BeforeEach
    void setUp() {
        recruiter = TestData.user(1L, Role.RECRUITER);
        candidate = TestData.user(2L, Role.CANDIDATE);
        openJob = TestData.job(10L, recruiter, JobStatus.OPEN);
    }

    // ---------- apply ----------

    @Test
    void apply_success_createsAppliedApplication() {
        when(jobRepository.findWithRecruiterById(10L)).thenReturn(Optional.of(openJob));
        when(applicationRepository.existsByJobIdAndCandidateId(10L, 2L)).thenReturn(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(candidate));
        when(applicationRepository.saveAndFlush(any(JobApplication.class))).thenAnswer(inv -> {
            JobApplication a = inv.getArgument(0);
            a.setId(100L);
            return a;
        });

        ApplicationResponse res = service.apply(2L, 10L, new ApplyRequest("Hire me"));

        assertThat(res.id()).isEqualTo(100L);
        assertThat(res.status()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(res.coverLetter()).isEqualTo("Hire me");
        assertThat(res.candidateId()).isEqualTo(2L);
        assertThat(res.jobId()).isEqualTo(10L);
    }

    @Test
    void apply_withoutBody_stillWorks() {
        when(jobRepository.findWithRecruiterById(10L)).thenReturn(Optional.of(openJob));
        when(applicationRepository.existsByJobIdAndCandidateId(10L, 2L)).thenReturn(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(candidate));
        when(applicationRepository.saveAndFlush(any(JobApplication.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.apply(2L, 10L, null).coverLetter()).isNull();
    }

    @Test
    void apply_duplicate_isRejected() {
        when(jobRepository.findWithRecruiterById(10L)).thenReturn(Optional.of(openJob));
        when(applicationRepository.existsByJobIdAndCandidateId(10L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> service.apply(2L, 10L, new ApplyRequest("x")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already applied");
        verify(applicationRepository, never()).saveAndFlush(any());
    }

    @Test
    void apply_raceCondition_dbConstraintIsTranslatedToConflict() {
        when(jobRepository.findWithRecruiterById(10L)).thenReturn(Optional.of(openJob));
        when(applicationRepository.existsByJobIdAndCandidateId(10L, 2L)).thenReturn(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(candidate));
        when(applicationRepository.saveAndFlush(any(JobApplication.class)))
                .thenThrow(new DataIntegrityViolationException("uq_application_job_candidate"));

        assertThatThrownBy(() -> service.apply(2L, 10L, new ApplyRequest("x")))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void apply_closedJob_violatesBusinessRule() {
        openJob.setStatus(JobStatus.CLOSED);
        when(jobRepository.findWithRecruiterById(10L)).thenReturn(Optional.of(openJob));

        assertThatThrownBy(() -> service.apply(2L, 10L, new ApplyRequest("x")))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void apply_unknownJob_notFound() {
        when(jobRepository.findWithRecruiterById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.apply(2L, 10L, null)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void apply_unknownCandidate_notFound() {
        when(jobRepository.findWithRecruiterById(10L)).thenReturn(Optional.of(openJob));
        when(applicationRepository.existsByJobIdAndCandidateId(10L, 2L)).thenReturn(false);
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.apply(2L, 10L, null)).isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- listing ----------

    @Test
    void listForCandidate_mapsAll() {
        when(applicationRepository.findByCandidateIdOrderByAppliedAtDesc(2L))
                .thenReturn(List.of(TestData.application(1L, openJob, candidate)));

        assertThat(service.listForCandidate(2L)).hasSize(1);
    }

    @Test
    void listForJob_ownerSeesApplicants() {
        when(jobRepository.findWithRecruiterById(10L)).thenReturn(Optional.of(openJob));
        when(applicationRepository.findByJobIdOrderByAppliedAtDesc(10L))
                .thenReturn(List.of(TestData.application(1L, openJob, candidate)));

        List<ApplicationResponse> res = service.listForJob(1L, 10L);

        assertThat(res).hasSize(1);
        assertThat(res.get(0).candidateEmail()).isEqualTo("user2@test.com");
    }

    @Test
    void listForJob_otherRecruiter_isForbidden() {
        when(jobRepository.findWithRecruiterById(10L)).thenReturn(Optional.of(openJob));

        assertThatThrownBy(() -> service.listForJob(99L, 10L)).isInstanceOf(ForbiddenException.class);
        verify(applicationRepository, never()).findByJobIdOrderByAppliedAtDesc(any());
    }

    @Test
    void listForJob_unknownJob_notFound() {
        when(jobRepository.findWithRecruiterById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listForJob(1L, 10L)).isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- status update ----------

    @Test
    void updateStatus_ownerCanMovePipeline() {
        JobApplication app = TestData.application(50L, openJob, candidate);
        when(applicationRepository.findWithDetailsById(50L)).thenReturn(Optional.of(app));

        ApplicationResponse res = service.updateStatus(1L, 50L, ApplicationStatus.INTERVIEW);

        assertThat(res.status()).isEqualTo(ApplicationStatus.INTERVIEW);
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.INTERVIEW);
    }

    @Test
    void updateStatus_otherRecruiter_isForbidden() {
        JobApplication app = TestData.application(50L, openJob, candidate);
        when(applicationRepository.findWithDetailsById(50L)).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> service.updateStatus(99L, 50L, ApplicationStatus.REJECTED))
                .isInstanceOf(ForbiddenException.class);
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.APPLIED);
    }

    @Test
    void updateStatus_notFound() {
        when(applicationRepository.findWithDetailsById(50L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateStatus(1L, 50L, ApplicationStatus.OFFERED))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- withdraw ----------

    @Test
    void withdraw_ownerDeletes() {
        JobApplication app = TestData.application(50L, openJob, candidate);
        when(applicationRepository.findWithDetailsById(50L)).thenReturn(Optional.of(app));

        service.withdraw(2L, 50L);

        verify(applicationRepository).delete(app);
    }

    @Test
    void withdraw_someoneElsesApplication_isForbidden() {
        JobApplication app = TestData.application(50L, openJob, candidate);
        when(applicationRepository.findWithDetailsById(50L)).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> service.withdraw(77L, 50L)).isInstanceOf(ForbiddenException.class);
        verify(applicationRepository, never()).delete(any());
    }

    @Test
    void withdraw_notFound() {
        when(applicationRepository.findWithDetailsById(50L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.withdraw(2L, 50L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
