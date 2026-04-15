package tn.esprit.freelanciajob.Service;

import org.springframework.data.domain.Page;
import tn.esprit.freelanciajob.Dto.JobStats;
import tn.esprit.freelanciajob.Dto.request.JobRequest;
import tn.esprit.freelanciajob.Dto.request.JobSearchRequest;
import tn.esprit.freelanciajob.Dto.response.JobResponse;
import tn.esprit.freelanciajob.Entity.Job;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface IJobService {
    Job addJob(JobRequest request);
    Job updateJob(Long id, JobRequest request);
    void deleteJob(Long id);
    Job getJobById(Long id);
    JobResponse getJobResponse(Long id);
    List<Job> getAllJobs();
    List<JobResponse> getAllJobResponses();
    List<JobResponse> getJobsByClientId(Long clientId);
    List<JobResponse> getRecommendedJobs(Long freelancerId);
    List<JobResponse> searchJobs(String keyword, String category, BigDecimal budgetMin,
                                  BigDecimal budgetMax, String locationType, Long skillId);
    Map<String, Long> getJobStatistics();
    List<JobStats> getJobsApplicationStats();
    Page<JobResponse> filterJobs(JobSearchRequest request);
}
