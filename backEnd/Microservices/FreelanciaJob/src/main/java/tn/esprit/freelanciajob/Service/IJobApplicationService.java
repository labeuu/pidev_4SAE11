package tn.esprit.freelanciajob.Service;

import tn.esprit.freelanciajob.Dto.request.JobApplicationRequest;
import tn.esprit.freelanciajob.Dto.response.JobApplicationResponse;
import tn.esprit.freelanciajob.Entity.JobApplication;
import tn.esprit.freelanciajob.Entity.Enums.ApplicationStatus;

import java.util.List;

public interface IJobApplicationService {
    JobApplicationResponse addApplication(JobApplicationRequest request);
    JobApplicationResponse updateApplication(Long id, JobApplicationRequest request);
    void deleteApplication(Long id);
    JobApplicationResponse getApplicationById(Long id);
    List<JobApplicationResponse> getAllApplications();
    List<JobApplicationResponse> getApplicationsByJob(Long jobId);
    List<JobApplicationResponse> getApplicationsByFreelancer(Long freelancerId);
    JobApplicationResponse updateStatus(Long id, ApplicationStatus status);
}
