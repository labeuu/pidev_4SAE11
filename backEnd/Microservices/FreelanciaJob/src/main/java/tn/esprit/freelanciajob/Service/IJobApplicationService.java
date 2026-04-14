package tn.esprit.freelanciajob.Service;

import org.springframework.web.multipart.MultipartFile;
import tn.esprit.freelanciajob.Dto.request.JobApplicationRequest;
import tn.esprit.freelanciajob.Dto.response.ApplyJobResponse;
import tn.esprit.freelanciajob.Dto.response.AttachmentResponse;
import tn.esprit.freelanciajob.Dto.response.JobApplicationResponse;
import tn.esprit.freelanciajob.Entity.Enums.ApplicationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface IJobApplicationService {

    // ── Existing CRUD (unchanged) ─────────────────────────────────────────────
    JobApplicationResponse addApplication(JobApplicationRequest request);
    JobApplicationResponse updateApplication(Long id, JobApplicationRequest request);
    void deleteApplication(Long id);
    JobApplicationResponse getApplicationById(Long id);
    List<JobApplicationResponse> getAllApplications();
    List<JobApplicationResponse> getApplicationsByJob(Long jobId);
    List<JobApplicationResponse> getApplicationsByFreelancer(Long freelancerId);
    JobApplicationResponse updateStatus(Long id, ApplicationStatus status);

    // ── New: enhanced apply workflow with file attachments ────────────────────
    ApplyJobResponse applyToJob(Long jobId, Long freelancerId, String proposalMessage,
                                BigDecimal expectedRate, LocalDate availabilityStart,
                                List<MultipartFile> files);

    /** Returns all attachments for an application (used by admin download). */
    List<AttachmentResponse> getAttachments(Long applicationId);
}
