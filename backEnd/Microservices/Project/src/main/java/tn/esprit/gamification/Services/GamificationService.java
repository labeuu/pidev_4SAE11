package tn.esprit.gamification.Services;

public interface GamificationService {
    void handleProjectCompleted(Long userId);
    void handleProjectCreated(Long userId);
    void handleFastResponse(Long userId); // appelé quand réponse rapide détectée
}
