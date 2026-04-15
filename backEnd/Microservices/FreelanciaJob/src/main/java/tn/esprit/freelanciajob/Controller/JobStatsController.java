package tn.esprit.freelanciajob.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.freelanciajob.Dto.response.JobStatsDTO;
import tn.esprit.freelanciajob.Service.JobStatsService;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class JobStatsController {

    private final JobStatsService statsService;

    /** GET /api/admin/job-stats — returns aggregated dashboard KPIs. */
    @GetMapping("/job-stats")
    public ResponseEntity<JobStatsDTO> getJobStats() {
        return ResponseEntity.ok(statsService.getStats());
    }
}
