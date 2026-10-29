package com.jobtracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jobtracker.dto.JobRequest;
import com.jobtracker.dto.JobResponse;
import com.jobtracker.dto.PageResponse;
import com.jobtracker.entity.JobPosting;
import com.jobtracker.entity.JobStatus;
import com.jobtracker.entity.Role;
import com.jobtracker.entity.User;
import com.jobtracker.exception.ForbiddenException;
import com.jobtracker.exception.ResourceNotFoundException;
import com.jobtracker.repository.JobPostingRepository;
import com.jobtracker.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class JobPostingServiceTest {

    @Mock JobPostingRepository jobRepository;
    @Mock UserRepository userRepository;
    @InjectMocks JobPostingService service;

    User recruiter;
    User otherRecruiter;

    @BeforeEach
    void setUp() {
        recruiter = TestData.user(1L, Role.RECRUITER);
        otherRecruiter = TestData.user(2L, Role.RECRUITER);
    }

    private JobRequest request(JobStatus status) {
        return new JobRequest("  Backend Dev ", "Acme", "Remote", "Great job", "10-20 LPA", status);
    }

    @Test
    void create_defaultsToOpen_andTrimsFields() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(recruiter));
        when(jobRepository.save(any(JobPosting.class))).thenAnswer(inv -> {
            JobPosting j = inv.getArgument(0);
            j.setId(10L);
            return j;
        });

        JobResponse res = service.create(1L, request(null));

        assertThat(res.id()).isEqualTo(10L);
        assertThat(res.title()).isEqualTo("Backend Dev");
        assertThat(res.status()).isEqualTo(JobStatus.OPEN);
        assertThat(res.recruiterId()).isEqualTo(1L);
    }

    @Test
    void create_respectsExplicitStatus() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(recruiter));
        when(jobRepository.save(any(JobPosting.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.create(1L, request(JobStatus.CLOSED)).status()).isEqualTo(JobStatus.CLOSED);
    }

    @Test
    void create_unknownRecruiter_throwsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(99L, request(null)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(jobRepository, never()).save(any());
    }

    @Test
    void getById_found() {
        when(jobRepository.findWithRecruiterById(5L))
                .thenReturn(Optional.of(TestData.job(5L, recruiter, JobStatus.OPEN)));

        assertThat(service.getById(5L).id()).isEqualTo(5L);
    }

    @Test
    void getById_notFound() {
        when(jobRepository.findWithRecruiterById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(5L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void searchOpenJobs_buildsLowercasePattern_andClampsPageSize() {
        Page<JobPosting> page = new PageImpl<>(List.of(TestData.job(1L, recruiter, JobStatus.OPEN)));
        when(jobRepository.search(eq(JobStatus.OPEN), eq("%java%"), any(Pageable.class))).thenReturn(page);

        PageResponse<JobResponse> res = service.searchOpenJobs("  JAVA ", -3, 1000);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(jobRepository).search(eq(JobStatus.OPEN), eq("%java%"), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(50);
        assertThat(res.content()).hasSize(1);
        assertThat(res.totalElements()).isEqualTo(1);
    }

    @Test
    void searchOpenJobs_nullKeyword_matchesEverything() {
        when(jobRepository.search(eq(JobStatus.OPEN), eq("%%"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        assertThat(service.searchOpenJobs(null, 0, 0).content()).isEmpty();

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(jobRepository).search(eq(JobStatus.OPEN), eq("%%"), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(1);
    }

    @Test
    void listByRecruiter_mapsAll() {
        when(jobRepository.findByRecruiterIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(
                TestData.job(1L, recruiter, JobStatus.OPEN), TestData.job(2L, recruiter, JobStatus.CLOSED)));

        assertThat(service.listByRecruiter(1L)).extracting(JobResponse::id).containsExactly(1L, 2L);
    }

    @Test
    void update_byOwner_changesFields() {
        JobPosting job = TestData.job(5L, recruiter, JobStatus.OPEN);
        when(jobRepository.findWithRecruiterById(5L)).thenReturn(Optional.of(job));

        JobResponse res = service.update(1L, 5L, request(JobStatus.CLOSED));

        assertThat(res.title()).isEqualTo("Backend Dev");
        assertThat(job.getStatus()).isEqualTo(JobStatus.CLOSED);
    }

    @Test
    void update_withoutStatus_keepsExistingStatus() {
        JobPosting job = TestData.job(5L, recruiter, JobStatus.CLOSED);
        when(jobRepository.findWithRecruiterById(5L)).thenReturn(Optional.of(job));

        service.update(1L, 5L, request(null));

        assertThat(job.getStatus()).isEqualTo(JobStatus.CLOSED);
    }

    @Test
    void update_byNonOwner_isForbidden() {
        JobPosting job = TestData.job(5L, recruiter, JobStatus.OPEN);
        when(jobRepository.findWithRecruiterById(5L)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> service.update(otherRecruiter.getId(), 5L, request(null)))
                .isInstanceOf(ForbiddenException.class);
        assertThat(job.getTitle()).isEqualTo("Java Dev"); // untouched
    }

    @Test
    void update_notFound() {
        when(jobRepository.findWithRecruiterById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(1L, 5L, request(null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_byOwner_deletes() {
        JobPosting job = TestData.job(5L, recruiter, JobStatus.OPEN);
        when(jobRepository.findWithRecruiterById(5L)).thenReturn(Optional.of(job));

        service.delete(1L, 5L);

        verify(jobRepository).delete(job);
    }

    @Test
    void delete_byNonOwner_isForbidden() {
        JobPosting job = TestData.job(5L, recruiter, JobStatus.OPEN);
        when(jobRepository.findWithRecruiterById(5L)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> service.delete(2L, 5L)).isInstanceOf(ForbiddenException.class);
        verify(jobRepository, never()).delete(any());
    }
}
