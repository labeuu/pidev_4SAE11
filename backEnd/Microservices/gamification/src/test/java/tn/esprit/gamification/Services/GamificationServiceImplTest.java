package tn.esprit.gamification.Services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.gamification.Entities.Achievement;
import tn.esprit.gamification.Entities.Enums.TargetRole;
import tn.esprit.gamification.Entities.Enums.conditionType;
import tn.esprit.gamification.Entities.UserLevel;
import tn.esprit.gamification.Evaluator.AchievementEvaluatorRegistry;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GamificationServiceImplTest {

    @Mock
    private AchievementService achievementService;

    @Mock
    private UserAchievementService userAchievementService;

    @Mock
    private UserLevelService userLevelService;

    @Mock
    private AchievementEvaluatorRegistry evaluatorRegistry;

    @InjectMocks
    private GamificationServiceImpl service;

    @Test
    void handleProjectCompleted_unlocksMatchingAchievements() {
        UserLevel ul = new UserLevel();
        ul.setUserId(1L);
        ul.setUserRole("FREELANCER");
        when(userLevelService.getUserLevel(1L)).thenReturn(ul);

        Achievement a = new Achievement();
        a.setId(10L);
        a.setConditionType(conditionType.PROJECT_COMPLETED);
        a.setConditionThreshold(5);
        a.setTargetRole(TargetRole.FREELANCER);

        when(achievementService.getByType(conditionType.PROJECT_COMPLETED)).thenReturn(List.of(a));
        when(achievementService.getByType(conditionType.FIRST_PROJECT)).thenReturn(List.of());

        // Evaluator says user has 6 completed projects (>= threshold)
        when(evaluatorRegistry.evaluate(conditionType.PROJECT_COMPLETED, 1L)).thenReturn(6);

        service.handleProjectCompleted(1L);

        verify(userAchievementService).unlockAchievement(1L, 10L);
    }

    @Test
    void handleProjectCreated_doesNotUnlockIfThresholdNotMet() {
        UserLevel ul = new UserLevel();
        ul.setUserId(1L);
        ul.setUserRole("CLIENT");
        when(userLevelService.getUserLevel(1L)).thenReturn(ul);

        Achievement a = new Achievement();
        a.setId(20L);
        a.setConditionType(conditionType.PROJECT_CREATED);
        a.setConditionThreshold(5);

        when(achievementService.getByType(conditionType.PROJECT_CREATED)).thenReturn(List.of(a));

        // Evaluator says 3 created ( < 5 threshold)
        when(evaluatorRegistry.evaluate(conditionType.PROJECT_CREATED, 1L)).thenReturn(3);

        service.handleProjectCreated(1L);

        verify(userAchievementService, never()).unlockAchievement(anyLong(), anyLong());
    }

    @Test
    void handleFastResponse_nominalCase() {
        UserLevel ul = new UserLevel();
        ul.setUserId(1L);
        ul.setUserRole("FREELANCER");
        when(userLevelService.getUserLevel(1L)).thenReturn(ul);

        Achievement a = new Achievement();
        a.setId(30L);
        a.setConditionType(conditionType.FAST_RESPONDER);
        a.setConditionThreshold(1);

        when(achievementService.getByType(conditionType.FAST_RESPONDER)).thenReturn(List.of(a));
        when(evaluatorRegistry.evaluate(conditionType.FAST_RESPONDER, 1L)).thenReturn(1);

        service.handleFastResponse(1L);

        verify(userLevelService).incrementFastResponderStreak(1L);
        verify(userAchievementService).unlockAchievement(1L, 30L);
    }

    @Test
    void recomputeAllAchievements_evaluatesAllCatalogs() {
        UserLevel ul = new UserLevel();
        ul.setUserId(1L);
        ul.setUserRole("FREELANCER");
        when(userLevelService.getUserLevel(1L)).thenReturn(ul);

        Achievement a = new Achievement();
        a.setId(40L);
        a.setConditionType(conditionType.XP_REACHED);
        a.setConditionThreshold(100);

        when(achievementService.getAll()).thenReturn(List.of(a));
        when(evaluatorRegistry.evaluate(conditionType.XP_REACHED, 1L)).thenReturn(150);

        service.recomputeAllAchievements(1L);

        verify(userAchievementService).unlockAchievement(1L, 40L);
    }
}
