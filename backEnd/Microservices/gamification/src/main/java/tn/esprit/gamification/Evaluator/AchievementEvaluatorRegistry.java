package tn.esprit.gamification.Evaluator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.gamification.Entities.Enums.conditionType;

import java.util.List;

/**
 * Registre qui centralise tous les évaluateurs et dispatch l'appel 
 * vers celui qui supporte le type de condition demandé.
 */
@Service
@RequiredArgsConstructor
public class AchievementEvaluatorRegistry {

    private final List<AchievementEvaluator> evaluators;

    public int evaluate(conditionType type, Long userId) {
        return evaluators.stream()
                .filter(e -> e.supports(type))
                .findFirst()
                .map(e -> e.getCurrentValue(userId))
                .orElse(0); 
    }
}
