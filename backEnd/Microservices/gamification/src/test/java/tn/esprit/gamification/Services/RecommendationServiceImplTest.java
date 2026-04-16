package tn.esprit.gamification.Services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.gamification.Dto.RecommendationDTO;
import tn.esprit.gamification.Entities.Achievement;
import tn.esprit.gamification.Entities.Enums.TargetRole;
import tn.esprit.gamification.Entities.Enums.conditionType;
import tn.esprit.gamification.Entities.UserAchievement;
import tn.esprit.gamification.Entities.UserLevel;
import tn.esprit.gamification.Evaluator.AchievementEvaluatorRegistry;
import tn.esprit.gamification.Repository.AchievementRepository;
import tn.esprit.gamification.Repository.UserAchievementRepository;
import tn.esprit.gamification.Repository.UserLevelRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceImplTest {

    @Mock
    private AchievementRepository achievementRepository;

    @Mock
    private UserAchievementRepository userAchievementRepository;

    @Mock
    private UserLevelRepository userLevelRepository;

    @Mock
    private AchievementEvaluatorRegistry evaluatorRegistry;

    @InjectMocks
    private RecommendationServiceImpl service;

    @Test
    void getRecommendations_newClient_baseRecommendations() {
        UserLevel ul = new UserLevel();
        ul.setUserId(1L);
        ul.setUserRole("CLIENT");
        ul.setXp(100);
        ul.setFastResponderStreak(1);

        when(userLevelRepository.findByUserId(1L)).thenReturn(Optional.of(ul));
        when(achievementRepository.findAll()).thenReturn(List.of());
        when(userAchievementRepository.findByUserId(1L)).thenReturn(List.of());

        List<RecommendationDTO> result = service.getRecommendations(1L);

        assertThat(result).isNotEmpty();
        // Priorities check:
        // Priority 1 for new user (no Newcomer unlocked)
        // Priority 2 for fast responder streak < 3
        // Priority 3 for limited XP (< 500)
        assertThat(result).anyMatch(r -> r.getPriority() == 1 && r.getMessage().contains("Opportunity"));
    }

    @Test
    void getRecommendations_withMissingAchievements() {
        UserLevel ul = new UserLevel();
        ul.setUserId(1L);
        ul.setUserRole("FREELANCER");

        Achievement unlockedNewcomer = new Achievement();
        unlockedNewcomer.setId(10L);
        unlockedNewcomer.setTitle("Newcomer");

        UserAchievement ua = new UserAchievement();
        ua.setAchievement(unlockedNewcomer);

        Achievement missing = new Achievement();
        missing.setId(20L);
        missing.setTitle("Pro");
        missing.setConditionType(conditionType.PROJECT_COMPLETED);
        missing.setConditionThreshold(10);
        missing.setTargetRole(TargetRole.FREELANCER);

        when(userLevelRepository.findByUserId(1L)).thenReturn(Optional.of(ul));
        when(achievementRepository.findAll()).thenReturn(List.of(unlockedNewcomer, missing));
        when(userAchievementRepository.findByUserId(1L)).thenReturn(List.of(ua));

        // Let's say user has 8 projects completed out of 10. (high priority)
        when(evaluatorRegistry.evaluate(conditionType.PROJECT_COMPLETED, 1L)).thenReturn(8);

        List<RecommendationDTO> result = service.getRecommendations(1L);

        // Progress = 8/10 = 0.8 -> >= 0.7 -> priority 1
        assertThat(result).anyMatch(r -> r.getPriority() == 1 && r.getMessage().contains("Complete 2 more projects"));
    }

    @Test
    void getRecommendations_noUserLevel_returnsFallback() {
        when(userLevelRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(achievementRepository.findAll()).thenReturn(List.of());
        when(userAchievementRepository.findByUserId(1L)).thenReturn(List.of());

        List<RecommendationDTO> result = service.getRecommendations(1L);

        assertThat(result).isNotEmpty();
        assertThat(result).anyMatch(r -> r.getMessage().contains("Join a project"));
    }
}
