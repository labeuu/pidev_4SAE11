package tn.esprit.freelanciajob.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.freelanciajob.Entity.JobApplication;

import java.util.List;
import java.util.Optional;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    List<JobApplication> findByJobId(Long jobId);

    List<JobApplication> findByFreelancerId(Long freelancerId);

    boolean existsByJobIdAndFreelancerId(Long jobId, Long freelancerId);

    Optional<JobApplication> findByJobIdAndFreelancerId(Long jobId, Long freelancerId);
}
