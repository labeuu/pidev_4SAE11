package tn.esprit.gamification.Dto;

import lombok.*;

/**
 * DTO exposant la progression d'un utilisateur vers un achievement spécifique.
 * Utilisé par GET /api/user-achievements/{userId}/progress
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AchievementProgressDTO {

    private Long achievementId;
    private String title;
    private String description;
    private String iconEmoji;
    private String conditionType;
    private String targetRole; // 🆕

    /** Valeur actuelle de l'utilisateur (ex: 3 projets complétés). */
    private int currentValue;

    /** Valeur cible définie sur l'achievement (ex: 5). */
    private int targetValue;

    /** Pourcentage arrondi : min(100, currentValue * 100 / targetValue). */
    private int progressPercent;

    /** XP gagnés si débloqué. */
    private int xpReward;

    /** true si l'utilisateur a déjà débloqué cet achievement. */
    private boolean unlocked;

    /** Date de déverrouillage (null si pas encore débloqué). */
    private String unlockedAt;
}
