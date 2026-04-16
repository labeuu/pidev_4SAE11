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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TopFreelancerSchedulerTest {

    @Mock
    private UserLevelService userLevelService;

    @Mock
    private AchievementService achievementService;

    @Mock
    private UserAchievementService userAchievementService;

    @Mock
    private GamificationNotificationService notificationService;

    @InjectMocks
    private TopFreelancerScheduler scheduler;

    @Test
    void detectAndRewardTopFreelancer_noUsers_doesNothing() {
        when(userLevelService.getAllUserLevels()).thenReturn(List.of());

        scheduler.detectAndRewardTopFreelancer();

        verify(userLevelService, never()).setTopFreelancer(anyLong(), anyBoolean());
    }

    @Test
    void detectAndRewardTopFreelancer_noFreelancersWithXp_doesNothing() {
        UserLevel ul = new UserLevel();
        ul.setUserId(1L);
        ul.setUserRole("CLIENT"); // Only FREELANCER is allowed
        ul.setXp(500);

        when(userLevelService.getAllUserLevels()).thenReturn(List.of(ul));

        scheduler.detectAndRewardTopFreelancer();

        verify(userLevelService, never()).setTopFreelancer(anyLong(), anyBoolean());
    }

    @Test
    void detectAndRewardTopFreelancer_nominalCase_crownsTopAndRevokesOld() {
        UserLevel oldTop = new UserLevel();
        oldTop.setUserId(1L);
        oldTop.setUserRole("FREELANCER");
        oldTop.setXp(100);

        UserLevel currentTop = new UserLevel();
        currentTop.setUserId(2L);
        currentTop.setUserRole("FREELANCER");
        currentTop.setXp(200); // 2L has more XP

        when(userLevelService.getAllUserLevels()).thenReturn(List.of(oldTop, currentTop));

        // Let's say 1L was previously designated top freelancer
        when(userLevelService.getCurrentTopFreelancers()).thenReturn(List.of(oldTop));

        Achievement topAchievement = new Achievement();
        topAchievement.setId(10L);
        when(achievementService.getByType(conditionType.TOP_FREELANCER)).thenReturn(List.of(topAchievement));

        scheduler.detectAndRewardTopFreelancer();

        // Old top freelancer should lose the crown
        verify(userLevelService).setTopFreelancer(1L, false);
        verify(notificationService).notifyTopFreelancerRevoked(1L);

        // New top freelancer should get the crown
        verify(userLevelService).setTopFreelancer(2L, true);
        verify(userAchievementService).unlockAchievement(2L, 10L);
        verify(notificationService).notifyTopFreelancerCrowned(2L, 200);
    }
}
