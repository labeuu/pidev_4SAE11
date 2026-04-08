package tn.esprit.freelanciajob.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.freelanciajob.Dto.request.JobApplicationRequest;
import tn.esprit.freelanciajob.Dto.response.JobApplicationResponse;
import tn.esprit.freelanciajob.Entity.Enums.ApplicationStatus;
import tn.esprit.freelanciajob.Service.IJobApplicationService;

import java.util.List;

@RestController
@RequestMapping("/job-applications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class JobApplicationController {

    private final IJobApplicationService applicationService;

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
}
