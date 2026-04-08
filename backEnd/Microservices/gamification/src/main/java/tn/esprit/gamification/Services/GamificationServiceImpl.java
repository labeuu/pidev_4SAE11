package tn.esprit.gamification.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.esprit.gamification.Entities.Achievement;
import tn.esprit.gamification.Entities.Enums.conditionType;
import tn.esprit.gamification.Evaluator.AchievementEvaluatorRegistry;

import java.util.List;

@Service
public class GamificationServiceImpl implements GamificationService {

    @Autowired
    private AchievementService achievementService;

    @Autowired
    private UserAchievementService userAchievementService;

    @Autowired
    private UserLevelService UserLevelService;

    @Autowired
    private AchievementEvaluatorRegistry evaluatorRegistry;

    @Override
    public void handleProjectCompleted(Long userId) {

        // 🎯 récupérer achievements liés à la complétion de projets
        List<Achievement> achievements =
                achievementService.getByType(conditionType.PROJECT_COMPLETED);

        // Ajouter "FIRST_PROJECT" aussi s'il n'est pas déjà dans la liste
        List<Achievement> firstProjectSpecs = achievementService.getByType(conditionType.FIRST_PROJECT);

        evaluateAndUnlock(userId, achievements);
        evaluateAndUnlock(userId, firstProjectSpecs);
    }

    @Override
    public void handleProjectCreated(Long userId) {

        List<Achievement> achievements =
                achievementService.getByType(conditionType.PROJECT_CREATED);

        evaluateAndUnlock(userId, achievements);
    }

    @Override
    public void handleFastResponse(Long userId) {
        UserLevelService.incrementFastResponderStreak(userId);
        
        // On évalue les achievements liés aux réponses rapides
        List<Achievement> achievements = achievementService.getByType(conditionType.FAST_RESPONDER);
        evaluateAndUnlock(userId, achievements);
    }

    @Override
    public void recomputeAllAchievements(Long userId) {
        // On récupère TOUT le catalogue
        List<Achievement> fullCatalog = achievementService.getAll();
        
        // On évalue TOUT. La méthode evaluateAndUnlock ignore 
        // ceux déjà possédés via userAchievementService.
        evaluateAndUnlock(userId, fullCatalog);
    }

    /**
     * Evalue chaque achievement potentiel par rapport aux données réelles de l'utilisateur.
     * Si currentValue >= threshold, on débloque.
     */
    private void evaluateAndUnlock(Long userId, List<Achievement> potentialAchievements) {
        if (potentialAchievements.isEmpty()) return;
        
        // 🆕 Check user role from UserLevel (cached locally)
        tn.esprit.gamification.Entities.UserLevel ul = UserLevelService.getUserLevel(userId);
        
        // Self-repair: Si le rôle est encore inconnu, on force une récupération pour l'évaluation
        if (ul.getUserRole() == null || ul.getUserRole().isEmpty()) {
            ul = UserLevelService.getUserLevel(userId); 
        }
        
        String userRole = (ul.getUserRole() != null) ? ul.getUserRole().toUpperCase() : "ALL";

        for (Achievement a : potentialAchievements) {
            // 🆕 Role check logic (Plus robuste + Logs)
            tn.esprit.gamification.Entities.Enums.TargetRole target = a.getTargetRole();
            String targetStr = target != null ? target.name() : "ALL";
            
            boolean roleMatches = "ALL".equalsIgnoreCase(targetStr) || 
                                targetStr.equalsIgnoreCase(userRole);
            
            if (!roleMatches) {
                continue; 
            }

            int current = evaluatorRegistry.evaluate(a.getConditionType(), userId);
            if (current >= a.getConditionThreshold()) {
                userAchievementService.unlockAchievement(userId, a.getId());
            }
        }
    }
}
