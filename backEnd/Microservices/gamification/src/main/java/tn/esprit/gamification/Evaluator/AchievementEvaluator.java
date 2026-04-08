package tn.esprit.gamification.Evaluator;

import tn.esprit.gamification.Entities.Enums.conditionType;

/**
 * Interface Strategy pour évaluer la progression d'un utilisateur 
 * vers un type d'achievement spécifique.
 */
public interface AchievementEvaluator {

    /** Retourne true si cette stratégie supporte le type de condition donné. */
    boolean supports(conditionType type);

    /** Récupère la valeur actuelle de l'utilisateur pour ce type de condition. */
    int getCurrentValue(Long userId);
}
