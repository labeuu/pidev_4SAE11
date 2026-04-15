package tn.esprit.freelanciajob.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.freelanciajob.Dto.request.JobApplicationRequest;
import tn.esprit.freelanciajob.Dto.response.ApplyJobResponse;
import tn.esprit.freelanciajob.Dto.response.AttachmentResponse;
import tn.esprit.freelanciajob.Dto.response.JobApplicationResponse;
import tn.esprit.freelanciajob.Entity.Enums.ApplicationStatus;
import tn.esprit.freelanciajob.Repository.ApplicationAttachmentRepository;
import tn.esprit.freelanciajob.Service.IJobApplicationService;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/job-applications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class JobApplicationController {

    private final IJobApplicationService          applicationService;
    private final ApplicationAttachmentRepository attachmentRepository;

    @Value("${app.upload.base-dir:uploads}")
    private String uploadBaseDir;

    // ── Existing CRUD endpoints (unchanged) ───────────────────────────────────

    @PostMapping("/add")
    public ResponseEntity<JobApplicationResponse> addApplication(
            @Valid @RequestBody JobApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(applicationService.addApplication(request));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<JobApplicationResponse> updateApplication(
            @PathVariable Long id, @Valid @RequestBody JobApplicationRequest request) {
        return ResponseEntity.ok(applicationService.updateApplication(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(@PathVariable Long id) {
        applicationService.deleteApplication(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobApplicationResponse> getApplicationById(@PathVariable Long id) {
        return ResponseEntity.ok(applicationService.getApplicationById(id));
    }

    @GetMapping("/list")
    public ResponseEntity<List<JobApplicationResponse>> getAllApplications() {
        return ResponseEntity.ok(applicationService.getAllApplications());
    }

    @GetMapping("/job/{jobId}")
    public ResponseEntity<List<JobApplicationResponse>> getByJob(@PathVariable Long jobId) {
        return ResponseEntity.ok(applicationService.getApplicationsByJob(jobId));
    }

    @GetMapping("/freelancer/{freelancerId}")
    public ResponseEntity<List<JobApplicationResponse>> getByFreelancer(@PathVariable Long freelancerId) {
        return ResponseEntity.ok(applicationService.getApplicationsByFreelancer(freelancerId));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<JobApplicationResponse> updateStatus(
            @PathVariable Long id, @RequestParam String value) {
        ApplicationStatus status = ApplicationStatus.valueOf(value.toUpperCase());
        return ResponseEntity.ok(applicationService.updateStatus(id, status));
    }

    // ── New: apply with file attachments ──────────────────────────────────────

    /**
     * POST /job-applications/{jobId}/apply
     * Content-Type: multipart/form-data
     *
     * Fields:
     *   freelancerId     (required)
     *   proposalMessage  (required, min 20 chars)
     *   expectedRate     (optional)
     *   availabilityStart (optional, ISO date yyyy-MM-dd)
     *   files            (optional, up to 5 × 10 MB; PDF / PNG / JPG / DOC / DOCX)
     */
    @PostMapping(value = "/{jobId}/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApplyJobResponse> applyToJob(
            @PathVariable Long jobId,
            @RequestParam Long freelancerId,
            @RequestParam String proposalMessage,
            @RequestParam(required = false) BigDecimal expectedRate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate availabilityStart,
            @RequestParam(value = "files", required = false) List<MultipartFile> files) {

        ApplyJobResponse response = applicationService.applyToJob(
                jobId, freelancerId, proposalMessage, expectedRate, availabilityStart,
                files != null ? files : Collections.emptyList());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── Attachment queries ────────────────────────────────────────────────────

    /** GET /job-applications/{applicationId}/attachments — list metadata. */
    @GetMapping("/{applicationId}/attachments")
    public ResponseEntity<List<AttachmentResponse>> getAttachments(@PathVariable Long applicationId) {
        return ResponseEntity.ok(applicationService.getAttachments(applicationId));
    }

    /**
     * GET /job-applications/attachments/{attachmentId}/download
     * Streams the file with Content-Disposition: attachment for browser save-dialog.
     * Used by admins / clients who want to force-download rather than preview.
     */
    @GetMapping("/attachments/{attachmentId}/download")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long attachmentId) {
        var opt = attachmentRepository.findById(attachmentId);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        var att      = opt.get();
        String rel   = att.getFileUrl().replaceFirst("^/uploads/", "");
        Path filePath = Paths.get(uploadBaseDir, rel);
        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) return ResponseEntity.notFound().build();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + att.getFileName() + "\"")
                    .contentType(MediaType.parseMediaType(att.getFileType()))
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
