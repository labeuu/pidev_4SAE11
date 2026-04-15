package tn.esprit.gamification.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.esprit.gamification.Dto.AchievementProgressDTO;
import tn.esprit.gamification.Entities.Achievement;
import tn.esprit.gamification.Entities.UserAchievement;
import tn.esprit.gamification.Evaluator.AchievementEvaluatorRegistry;
import tn.esprit.gamification.Repository.AchievementRepository;
import tn.esprit.gamification.Repository.UserAchievementRepository;

import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserAchievementServiceImpl implements UserAchievementService {

    @Autowired
    private UserAchievementRepository repo;

    @Autowired
    private AchievementRepository achievementRepo;

    @Autowired
    private UserLevelService userLevelService;

    @Autowired
    private AchievementEvaluatorRegistry evaluatorRegistry;

    @Override
    @Transactional
    public void unlockAchievement(Long userId, Long achievementId) {

        Achievement a = achievementRepo.findById(achievementId).orElse(null);

        if (a == null) return;

        boolean exists = repo.existsByUserIdAndAchievement(userId, a);

        if (!exists) {
            UserAchievement ua = new UserAchievement();
            ua.setUserId(userId);
            ua.setAchievement(a);
            ua.setUnlockedAt(LocalDateTime.now());

            repo.save(ua);

            // 🔥 add XP
            userLevelService.addXp(userId, a.getXpReward());
        }
    }

    @Override
    public List<UserAchievement> getUserAchievements(Long userId) {
        return repo.findByUserId(userId);
    }

    @Override
    public List<AchievementProgressDTO> getUserProgress(Long userId) {
        // 1. Tous les achievements possibles (catalogue)
        List<Achievement> catalog = achievementRepo.findAll();
        
        // 2. Achievements déjà débloqués par l'user
        List<UserAchievement> unlockedRows = repo.findByUserId(userId);
        Map<Long, UserAchievement> unlockedMap = unlockedRows.stream()
                .filter(ua -> ua != null && ua.getAchievement() != null && ua.getAchievement().getId() != null)
                .collect(Collectors.toMap(
                        ua -> ua.getAchievement().getId(),
                        ua -> ua,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));

        // 3. Mapper vers DTO avec évaluation en temps réel pour ceux non débloqués
        return catalog.stream().map(a -> {
            int currentVal = 0;
            try {
                currentVal = evaluatorRegistry.evaluate(a.getConditionType(), userId);
            } catch (Exception ignored) {
                currentVal = 0;
            }
            int target = a.getConditionThreshold();
            
            // 🆕 LOGIQUE DE RATTRAPAGE : Si on a atteint le seuil mais le badge n'est pas marqué débloqué
            boolean isUnlocked = unlockedMap.containsKey(a.getId());
            if (!isUnlocked && currentVal >= target && target > 0) {
                try {
                    this.unlockAchievement(userId, a.getId());
                    isUnlocked = true; // On marque comme débloqué pour l'UI
                    UserAchievement justUnlocked = new UserAchievement();
                    justUnlocked.setAchievement(a);
                    justUnlocked.setUnlockedAt(LocalDateTime.now());
                    unlockedMap.put(a.getId(), justUnlocked);
                } catch (Exception e) {
                   // log ignore
                }
            }
            
            int percent = (target == 0) ? 100 : Math.min(100, (currentVal * 100) / target);

            return AchievementProgressDTO.builder()
                    .achievementId(a.getId())
                    .title(a.getTitle())
                    .description(a.getDescription())
                    .iconEmoji(a.getIconEmoji())
                    .conditionType(a.getConditionType() != null ? a.getConditionType().name() : "XP_REACHED")
                    .targetRole(a.getTargetRole() != null ? a.getTargetRole().toString().toUpperCase() : "ALL") // ⚡ Force conversion
                    .currentValue(currentVal)
                    .targetValue(target)
                    .progressPercent(percent)
                    .xpReward(a.getXpReward())
                    .unlocked(isUnlocked)
                    .unlockedAt(
                            isUnlocked && unlockedMap.get(a.getId()) != null && unlockedMap.get(a.getId()).getUnlockedAt() != null
                                    ? unlockedMap.get(a.getId()).getUnlockedAt().toString()
                                    : null
                    )
                    .build();
        }).collect(Collectors.toList());
    }
}
