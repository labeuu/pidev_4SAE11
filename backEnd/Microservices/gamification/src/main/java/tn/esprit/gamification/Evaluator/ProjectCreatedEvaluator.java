package tn.esprit.gamification.Evaluator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tn.esprit.gamification.Entities.Enums.conditionType;
import tn.esprit.gamification.client.ProjectClient;

@Component
@RequiredArgsConstructor
public class ProjectCreatedEvaluator implements AchievementEvaluator {

    private final ProjectClient projectClient;

    @Override
    public boolean supports(conditionType type) {
        return type == conditionType.PROJECT_CREATED;
    }

    @Override
    public int getCurrentValue(Long userId) {
        try {
            return (int) projectClient.countCreatedProjects(userId);
        } catch (Exception e) {
            return 0;
        }
    }
}
