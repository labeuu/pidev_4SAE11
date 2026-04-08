package tn.esprit.gamification.Evaluator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tn.esprit.gamification.Entities.Enums.conditionType;
import tn.esprit.gamification.client.ReviewClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ReviewGivenEvaluator implements AchievementEvaluator {

    private final ReviewClient reviewClient;

    @Override
    public boolean supports(conditionType type) {
        return type == conditionType.REVIEW_GIVEN;
    }

    @Override
    public int getCurrentValue(Long userId) {
        try {
            List<Object> reviews = reviewClient.getReviewsByReviewerId(userId);
            return reviews != null ? reviews.size() : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
