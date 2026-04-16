package tn.esprit.gamification.Services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.gamification.Dto.AchievementProgressDTO;
import tn.esprit.gamification.Entities.Achievement;
import tn.esprit.gamification.Entities.Enums.conditionType;
import tn.esprit.gamification.Entities.UserAchievement;
import tn.esprit.gamification.Evaluator.AchievementEvaluatorRegistry;
import tn.esprit.gamification.Repository.AchievementRepository;
import tn.esprit.gamification.Repository.UserAchievementRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAchievementServiceImplTest {

    @Mock
    private UserAchievementRepository repository;

    @Mock
    private AchievementRepository achievementRepository;

    @Mock
    private UserLevelService userLevelService;

    @Mock
    private AchievementEvaluatorRegistry evaluatorRegistry;

    @InjectMocks
    private UserAchievementServiceImpl service;

    @Test
    void unlockAchievement_whenNotExists_unlocksAndAddsXp() {
        Achievement a = new Achievement();
        a.setId(10L);
        a.setXpReward(50);
        when(achievementRepository.findById(10L)).thenReturn(Optional.of(a));
        when(repository.existsByUserIdAndAchievement(1L, a)).thenReturn(false);

        service.unlockAchievement(1L, 10L);

        verify(repository).save(any(UserAchievement.class));
        verify(userLevelService).addXp(1L, 50);
    }

    @Test
    void unlockAchievement_whenAlreadyExists_doesNothing() {
        Achievement a = new Achievement();
        a.setId(10L);
        when(achievementRepository.findById(10L)).thenReturn(Optional.of(a));
        when(repository.existsByUserIdAndAchievement(1L, a)).thenReturn(true);

        service.unlockAchievement(1L, 10L);

        verify(repository, never()).save(any(UserAchievement.class));
        verify(userLevelService, never()).addXp(anyLong(), anyInt());
    }

    @Test
    void getUserAchievements_nominalCase() {
        UserAchievement ua = new UserAchievement();
        ua.setId(100L);
        when(repository.findByUserId(1L)).thenReturn(List.of(ua));

        List<UserAchievement> result = service.getUserAchievements(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(100L);
    }

    @Test
    void getUserProgress_nominalCase_withCatchupLogic() {
        // Mock Catalog
        Achievement a1 = new Achievement();
        a1.setId(10L);
        a1.setConditionType(conditionType.PROJECT_COMPLETED);
        a1.setConditionThreshold(5);

        when(achievementRepository.findAll()).thenReturn(List.of(a1));

        // Mock already unlocked items
        // The user hasn't unlocked this yet according to db
        when(repository.findByUserId(1L)).thenReturn(List.of());

        // Mock evaluator returns 6 (currentVal > target, so it triggers catch up logic)
        when(evaluatorRegistry.evaluate(conditionType.PROJECT_COMPLETED, 1L)).thenReturn(6);

        // Required mocks for unlock logic during catchup
        when(achievementRepository.findById(10L)).thenReturn(Optional.of(a1));
        when(repository.existsByUserIdAndAchievement(1L, a1)).thenReturn(false);

        List<AchievementProgressDTO> progress = service.getUserProgress(1L);

        assertThat(progress).hasSize(1);
        assertThat(progress.get(0).getAchievementId()).isEqualTo(10L);
        assertThat(progress.get(0).getCurrentValue()).isEqualTo(6);
        assertThat(progress.get(0).getProgressPercent()).isEqualTo(100);
        assertThat(progress.get(0).isUnlocked()).isTrue();

        // Verify catchup unlocked it
        verify(repository).save(any(UserAchievement.class));
    }
}
