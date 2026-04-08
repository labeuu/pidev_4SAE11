package tn.esprit.gamification.Services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.gamification.Dto.RecommendationDTO;
import tn.esprit.gamification.Entities.Achievement;
import tn.esprit.gamification.Entities.Enums.conditionType;
import tn.esprit.gamification.Entities.UserAchievement;
import tn.esprit.gamification.Entities.UserLevel;
import tn.esprit.gamification.Evaluator.AchievementEvaluatorRegistry;
import tn.esprit.gamification.Repository.AchievementRepository;
import tn.esprit.gamification.Repository.UserAchievementRepository;
import tn.esprit.gamification.Repository.UserLevelRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final UserLevelRepository userLevelRepository;
    private final AchievementEvaluatorRegistry evaluatorRegistry;

    @Override
    public List<RecommendationDTO> getRecommendations(Long userId) {
        List<RecommendationDTO> recommendations = new ArrayList<>();

        // 1. Fetch user data
        UserLevel userLevel = userLevelRepository.findByUserId(userId).orElse(null);
        List<Achievement> allAchievements = achievementRepository.findAll();
        List<UserAchievement> unlockedUserAchievements = userAchievementRepository.findByUserId(userId);

        Set<Long> unlockedIds = unlockedUserAchievements.stream()
                .map(ua -> ua.getAchievement().getId())
                .collect(Collectors.toSet());

        boolean hasStarted = unlockedUserAchievements.stream()
                .anyMatch(ua -> {
                    String title = ua.getAchievement().getTitle();
                    return "Newcomer".equalsIgnoreCase(title) || "Opportunity Maker".equalsIgnoreCase(title);
                });
        
        String userRoleStr = (userLevel != null && userLevel.getUserRole() != null) 
                             ? userLevel.getUserRole().toUpperCase() : "ALL";

        // 2. Identify missing achievements with role matching
        List<Achievement> missingAchievements = allAchievements.stream()
                .filter(a -> !unlockedIds.contains(a.getId()))
                .filter(a -> {
                    String target = (a.getTargetRole() != null) ? a.getTargetRole().name() : "ALL";
                    return "ALL".equalsIgnoreCase(target) || target.equalsIgnoreCase(userRoleStr);
                })
                .collect(Collectors.toList());

        // 3. Rule-based analysis for missing achievements
        for (Achievement achievement : missingAchievements) {
            int currentValue = evaluatorRegistry.evaluate(achievement.getConditionType(), userId);
            int threshold = achievement.getConditionThreshold();

            if (currentValue < threshold) {
                int remaining = threshold - currentValue;
                double progress = (double) currentValue / threshold;

                String message = generateAchievementMessage(achievement, remaining);
                int priority = (progress >= 0.7) ? 1 : 2; // High priority if >= 70% progress

                recommendations.add(new RecommendationDTO(message, priority));
            }
        }

        // 4. Base recommendations (Daily activity & Onboarding)
        if (!hasStarted) {
            String startMsg = "CLIENT".equalsIgnoreCase(userRoleStr)
                ? "Start by posting your first project to unlock your first 'Opportunity' achievement! 🚀"
                : "Start by completing your first project to unlock your first 'Newcomer' achievement! 🚀";
            recommendations.add(new RecommendationDTO(startMsg, 1));
        }

        if (userLevel != null) {
            if (userLevel.getXp() < 500) {
                recommendations.add(new RecommendationDTO("Increase your activity by applying to more projects to gain more XP! 📈", 3));
            }
            if (userLevel.getFastResponderStreak() < 3) {
                recommendations.add(new RecommendationDTO("Stay active daily and respond quickly to build your streak! 🔥", 2));
            }
        } else {
            recommendations.add(new RecommendationDTO("Join a project to start your gamification journey! ✨", 1));
        }

        // 5. Cleanup, Sort and Limit
        return recommendations.stream()
                .distinct() // Avoid exact duplicates
                .sorted(Comparator.comparingInt(RecommendationDTO::getPriority))
                .limit(5)
                .collect(Collectors.toList());
    }

    private String generateAchievementMessage(Achievement a, int remaining) {
        switch (a.getConditionType()) {
            case PROJECT_COMPLETED:
            case FIRST_PROJECT:
                return String.format("Complete %d more project%s to unlock the '%s' badge!", 
                    remaining, (remaining > 1 ? "s" : ""), a.getTitle());
            case STREAK_DAYS:
            case FAST_RESPONDER:
                return String.format("You are only %d day%s away from unlocking '%s'! Keep it up!", 
                    remaining, (remaining > 1 ? "s" : ""), a.getTitle());
            case XP_REACHED:
                return String.format("Gain %d more XP points to reach your next milestone: %s!", 
                    remaining, a.getTitle());
            case PROJECT_CREATED:
                return String.format("Create %d more project%s to earn the '%s' badge!", 
                    remaining, (remaining > 1 ? "s" : ""), a.getTitle());
            default:
                return String.format("You're close to unlocking '%s', only %d more needed!", 
                    a.getTitle(), remaining);
        }
    }
}
