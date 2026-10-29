package com.jobtracker.repository;

import com.jobtracker.entity.JobPosting;
import com.jobtracker.entity.JobStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {

    /** {@code @EntityGraph} loads the recruiter in the same SQL query (avoids N+1 selects). */
    @EntityGraph(attributePaths = "recruiter")
    Optional<JobPosting> findWithRecruiterById(Long id);

    @EntityGraph(attributePaths = "recruiter")
    List<JobPosting> findByRecruiterIdOrderByCreatedAtDesc(Long recruiterId);

    /** {@code pattern} is already lower-cased and wrapped in % by the service. */
    @EntityGraph(attributePaths = "recruiter")
    @Query("""
            select j from JobPosting j
            where j.status = :status
              and (lower(j.title) like :pattern
                   or lower(j.company) like :pattern
                   or lower(j.location) like :pattern)
            order by j.createdAt desc
            """)
    Page<JobPosting> search(@Param("status") JobStatus status,
                            @Param("pattern") String pattern,
                            Pageable pageable);
}
