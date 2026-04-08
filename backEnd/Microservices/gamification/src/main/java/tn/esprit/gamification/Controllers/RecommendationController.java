package tn.esprit.gamification.Controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.gamification.Dto.RecommendationDTO;
import tn.esprit.gamification.Services.RecommendationService;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // 🛡️ Enable CORS for Angular frontend
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/{userId}")
    public ResponseEntity<List<RecommendationDTO>> getRecommendations(@PathVariable Long userId) {
        List<RecommendationDTO> recommendations = recommendationService.getRecommendations(userId);
        return ResponseEntity.ok(recommendations);
    }
}
