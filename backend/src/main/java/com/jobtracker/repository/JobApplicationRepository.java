package com.jobtracker.repository;

import com.jobtracker.entity.JobApplication;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    boolean existsByJobIdAndCandidateId(Long jobId, Long candidateId);

    @EntityGraph(attributePaths = {"job", "job.recruiter", "candidate"})
    Optional<JobApplication> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"job", "candidate"})
    List<JobApplication> findByCandidateIdOrderByAppliedAtDesc(Long candidateId);

    @EntityGraph(attributePaths = {"job", "candidate"})
    List<JobApplication> findByJobIdOrderByAppliedAtDesc(Long jobId);
}
