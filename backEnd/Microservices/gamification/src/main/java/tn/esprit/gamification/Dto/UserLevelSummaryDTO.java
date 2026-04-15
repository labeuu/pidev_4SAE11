package tn.esprit.gamification.Dto;

import lombok.*;

/**
 * DTO enrichi du niveau utilisateur incluant les métriques de progression.
 * Utilisé par GET /api/user-level/{userId}/summary
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLevelSummaryDTO {

    private Long userId;
    private int xp;
    private int level;

    /** XP accumulés dans le palier courant (entre 0 et xpNeededForNextLevel). */
    private int xpInCurrentTier;

    /** XP totaux nécessaires pour atteindre le prochain niveau depuis zéro. */
    private int xpToNextLevel;

    /** XP encore à gagner pour monter de niveau. */
    private int xpRemaining;

    /** Pourcentage de progression dans le palier courant (0-100). */
    private int progressPercent;

    private boolean isTopFreelancer;
    private int fastResponderStreak;
}
