package tn.esprit.gamification.Evaluator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tn.esprit.gamification.Entities.Enums.conditionType;
import tn.esprit.gamification.client.ProjectClient;

@Component
@RequiredArgsConstructor
public class ProjectCompletedEvaluator implements AchievementEvaluator {

    private final ProjectClient projectClient;

    @Override
    public boolean supports(conditionType type) {
        return type == conditionType.PROJECT_COMPLETED || type == conditionType.FIRST_PROJECT;
    }

    @Override
    public int getCurrentValue(Long userId) {
        try {
            return (int) projectClient.countCompletedProjects(userId);
        } catch (Exception e) {
            return 0; // Fallback in case of Feign error
        }
    }
}
