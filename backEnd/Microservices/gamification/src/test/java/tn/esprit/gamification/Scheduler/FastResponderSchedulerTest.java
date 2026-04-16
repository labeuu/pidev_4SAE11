package tn.esprit.gamification.Scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.gamification.Entities.Achievement;
import tn.esprit.gamification.Entities.Enums.conditionType;
import tn.esprit.gamification.Entities.UserLevel;
import tn.esprit.gamification.Services.AchievementService;
import tn.esprit.gamification.Services.GamificationNotificationService;
import tn.esprit.gamification.Services.UserAchievementService;
import tn.esprit.gamification.Services.UserLevelService;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FastResponderSchedulerTest {

    @Mock
    private UserLevelService userLevelService;

    @Mock
    private AchievementService achievementService;

    @Mock
    private UserAchievementService userAchievementService;

    @Mock
    private GamificationNotificationService gamificationNotificationService;

    @InjectMocks
    private FastResponderScheduler scheduler;

    @Test
    void checkFastResponders_noUsersMeetingStreak_doesNothing() {
        UserLevel ul = new UserLevel();
        ul.setUserId(1L);
        ul.setFastResponderStreak(2); // Less than 3

        when(userLevelService.getAllUserLevels()).thenReturn(List.of(ul));
        when(achievementService.getByType(conditionType.FAST_RESPONDER)).thenReturn(List.of());

        scheduler.checkFastResponders();

        verify(userLevelService, never()).resetFastResponderStreak(anyLong());
    }

    @Test
    void checkFastResponders_usersMeetStreak_unlocksBadgeAndResets_notifies() {
        UserLevel ul = new UserLevel();
        ul.setUserId(1L);
        ul.setFastResponderStreak(4); // >= 3

        when(userLevelService.getAllUserLevels()).thenReturn(List.of(ul));

        Achievement a = new Achievement();
        a.setId(10L);
        a.setTitle("Lightning Fast");
        when(achievementService.getByType(conditionType.FAST_RESPONDER)).thenReturn(List.of(a));

        scheduler.checkFastResponders();

        verify(userAchievementService).unlockAchievement(1L, 10L);
        verify(userLevelService).resetFastResponderStreak(1L);
        verify(gamificationNotificationService).notifyFastResponderBadge(1L, "Lightning Fast");
    }
}
