package tn.esprit.gamification.Evaluator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tn.esprit.gamification.Entities.Enums.conditionType;
import tn.esprit.gamification.Entities.UserLevel;
import tn.esprit.gamification.Services.UserLevelService;

@Component
@RequiredArgsConstructor
public class FastResponderEvaluator implements AchievementEvaluator {

    private final UserLevelService userLevelService;

    @Override
    public boolean supports(conditionType type) {
        return type == conditionType.FAST_RESPONDER;
    }

    @Override
    public int getCurrentValue(Long userId) {
        UserLevel ul = userLevelService.getUserLevel(userId);
        return ul != null ? ul.getFastResponderStreak() : 0;
    }
}
