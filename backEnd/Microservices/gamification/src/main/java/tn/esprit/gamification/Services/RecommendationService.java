package tn.esprit.gamification.Services;

import tn.esprit.gamification.Dto.RecommendationDTO;
import java.util.List;

public interface RecommendationService {
    List<RecommendationDTO> getRecommendations(Long userId);
}
