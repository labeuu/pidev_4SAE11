package tn.esprit.freelanciajob.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import tn.esprit.freelanciajob.Entity.JobApplication;

import java.util.List;
import java.util.Optional;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    List<JobApplication> findByJobId(Long jobId);

    List<JobApplication> findByFreelancerId(Long freelancerId);

    boolean existsByJobIdAndFreelancerId(Long jobId, Long freelancerId);

    Optional<JobApplication> findByJobIdAndFreelancerId(Long jobId, Long freelancerId);

    /** Number of distinct freelancers who have applied to at least one job. */
    @Query("SELECT COUNT(DISTINCT a.freelancerId) FROM JobApplication a")
    long countUniqueFreelancers();
}
