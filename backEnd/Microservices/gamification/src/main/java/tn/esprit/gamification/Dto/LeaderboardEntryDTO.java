package tn.esprit.gamification.Dto;

import lombok.*;

/**
 * DTO représentant une entrée dans le classement global des utilisateurs.
 * Utilisé par GET /api/user-level/leaderboard?top=N
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaderboardEntryDTO {

    /** Position dans le classement (1-indexed). */
    private int rank;

    private Long userId;
    private String fullName; // 🆕
    private int xp;
    private int level;
    private boolean isTopFreelancer;
    private int fastResponderStreak;
}
