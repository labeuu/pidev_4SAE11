package tn.esprit.freelanciajob.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.freelanciajob.Dto.JobStats;
import tn.esprit.freelanciajob.Dto.request.GenerateJobRequest;
import tn.esprit.freelanciajob.Dto.request.JobRequest;
import tn.esprit.freelanciajob.Dto.request.JobSearchRequest;
import tn.esprit.freelanciajob.Dto.request.TranslationRequest;
import tn.esprit.freelanciajob.Dto.response.FitScoreResponse;
import tn.esprit.freelanciajob.Dto.response.GeneratedJobDraft;
import tn.esprit.freelanciajob.Dto.response.JobResponse;
import tn.esprit.freelanciajob.Dto.response.TranslationResponse;
import tn.esprit.freelanciajob.Entity.Job;
import tn.esprit.freelanciajob.Service.AiJobGeneratorService;
import tn.esprit.freelanciajob.Service.IJobService;
import tn.esprit.freelanciajob.Service.ProfileFitScoreService;
import tn.esprit.freelanciajob.Service.TranslationService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class JobController {

    private final IJobService jobService;
    private final AiJobGeneratorService aiJobGeneratorService;
    private final TranslationService translationService;
    private final ProfileFitScoreService profileFitScoreService;

    @PostMapping("/translate")
    public ResponseEntity<TranslationResponse> translate(@Valid @RequestBody TranslationRequest request) {
        String src = (request.getSourceLang() != null && !request.getSourceLang().isBlank())
                ? request.getSourceLang() : "auto";
        String translated = translationService.translate(request.getText(), request.getTargetLang(), src);
        return ResponseEntity.ok(new TranslationResponse(translated));
    }

    @PostMapping("/generate")
    public ResponseEntity<GeneratedJobDraft> generateJobDraft(@Valid @RequestBody GenerateJobRequest request) {
        return ResponseEntity.ok(aiJobGeneratorService.generateJobDraft(request.getPrompt()));
    }

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

    /**
     * Advanced server-side filter with pagination.
     * Accepts all filter criteria + page/size/sort as a JSON body.
     * Returns Spring Data's Page<JobResponse> (includes totalElements, totalPages, etc.)
     */
    @PostMapping("/filter")
    public ResponseEntity<Page<JobResponse>> filterJobs(@Valid @RequestBody JobSearchRequest request) {
        try {
            return ResponseEntity.ok(jobService.filterJobs(request));
        } catch (Exception e) {
            log.error("[filterJobs] request={} | error={} | cause={}", request, e.getMessage(), e.getCause() != null ? e.getCause().getMessage() : "none", e);
            throw e;
        }
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

    @GetMapping("/{jobId}/fit-score")
    public ResponseEntity<FitScoreResponse> getFitScore(
            @PathVariable Long jobId,
            @RequestParam Long freelancerId) {
        return ResponseEntity.ok(profileFitScoreService.computeFitScore(jobId, freelancerId));
    }
}
