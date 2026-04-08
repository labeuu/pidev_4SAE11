package tn.esprit.freelanciajob.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.freelanciajob.Dto.JobStats;
import tn.esprit.freelanciajob.Dto.request.JobRequest;
import tn.esprit.freelanciajob.Dto.response.JobResponse;
import tn.esprit.freelanciajob.Entity.Job;
import tn.esprit.freelanciajob.Service.IJobService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class JobController {

    private final IJobService jobService;

    @PostMapping("/add")
    public ResponseEntity<Job> addJob(@Valid @RequestBody JobRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(jobService.addJob(request));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<Job> updateJob(@PathVariable Long id, @Valid @RequestBody JobRequest request) {
        return ResponseEntity.ok(jobService.updateJob(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable Long id) {
        jobService.deleteJob(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/list")
    public ResponseEntity<List<JobResponse>> getAllJobs() {
        return ResponseEntity.ok(jobService.getAllJobResponses());
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getJobById(@PathVariable Long id) {
        return ResponseEntity.ok(jobService.getJobResponse(id));
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<JobResponse>> getJobsByClient(@PathVariable Long clientId) {
        return ResponseEntity.ok(jobService.getJobsByClientId(clientId));
    }

    @GetMapping("/recommended")
    public ResponseEntity<List<JobResponse>> getRecommendedJobs(@RequestParam Long userId) {
        return ResponseEntity.ok(jobService.getRecommendedJobs(userId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<JobResponse>> searchJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal budgetMin,
            @RequestParam(required = false) BigDecimal budgetMax,
            @RequestParam(required = false) String locationType,
            @RequestParam(required = false) Long skillId) {
        return ResponseEntity.ok(
                jobService.searchJobs(keyword, category, budgetMin, budgetMax, locationType, skillId)
        );
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Long>> getStatistics() {
        return ResponseEntity.ok(jobService.getJobStatistics());
    }

    @GetMapping("/application-stats")
    public ResponseEntity<List<JobStats>> getApplicationStats() {
        return ResponseEntity.ok(jobService.getJobsApplicationStats());
    }
}
