package tn.esprit.gamification.Services;

public interface GamificationService {
    void handleProjectCompleted(Long userId);
    void handleProjectCreated(Long userId);
    void handleFastResponse(Long userId); // appelé quand réponse rapide détectée

    // 🆕 Réévaluer tout le catalogue pour un utilisateur (rétroactif/batch)
    void recomputeAllAchievements(Long userId);
}
