package tn.esprit.gamification.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import tn.esprit.gamification.Dto.LeaderboardEntryDTO;
import tn.esprit.gamification.Dto.UserLevelSummaryDTO;
import tn.esprit.gamification.Entities.UserLevel;
import tn.esprit.gamification.Repository.UserLevelRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserLevelServiceImpl implements UserLevelService {

    @Autowired
    private UserLevelRepository repo;
    
    @Autowired
    private tn.esprit.gamification.client.UserClient userClient;

    @Value("${gamification.xp.scale-factor:50}")
    private int xpScaleFactor;

    @Override
    public UserLevel getUserLevel(Long userId) {
        UserLevel ul = repo.findByUserId(userId).orElse(null);
        
        if (ul == null) {
            ul = new UserLevel();
            ul.setUserId(userId);
            fetchAndSetRole(ul);
            return repo.save(ul);
        }

        // 🆕 Si le rôle est manquant (ex: erreur de port précédente), on réessaie de le chercher
        if (ul.getUserRole() == null || ul.getUserRole().isEmpty()) {
            fetchAndSetRole(ul);
            return repo.save(ul);
        }

        return ul;
    }

    private void fetchAndSetRole(UserLevel ul) {
        try {
            tn.esprit.gamification.client.UserClient.UserResponseDTO res = userClient.getUserById(ul.getUserId());
            if (res != null && res.getRole() != null) {
                ul.setUserRole(res.getRole());
            }
        } catch (Exception e) {
            // log...
        }
    }

    @Override
    public void addXp(Long userId, int xp) {
        UserLevel ul = getUserLevel(userId);
        ul.setXp(ul.getXp() + xp);
        
        // 🆕 Formule racine carrée : level = floor(sqrt(xp/50)) + 1
        int newLevel = calculateLevel(ul.getXp());
        ul.setLevel(newLevel);

        // 🚀 FORCE UPDATE DIRECT (SQL)
        repo.updateXpAndLevel(userId, ul.getXp(), ul.getLevel());
    }

    @Override
    public UserLevelSummaryDTO getUserLevelSummary(Long userId) {
        UserLevel ul = getUserLevel(userId);
        int xp = ul.getXp();
        int currentLevel = ul.getLevel();

        // Seuil XP pour le niveau actuel et le prochain
        int xpCurrentLevel = 50 * (int)Math.pow(currentLevel - 1, 2);
        int xpNextLevel = 50 * (int)Math.pow(currentLevel, 2);

        int xpInCurrentTier = xp - xpCurrentLevel;
        int totalNeededInTier = xpNextLevel - xpCurrentLevel;
        int progressPercent = (totalNeededInTier == 0) ? 100 : Math.min(100, (xpInCurrentTier * 100) / totalNeededInTier);

        return UserLevelSummaryDTO.builder()
                .userId(userId)
                .xp(xp)
                .level(currentLevel)
                .xpInCurrentTier(xpInCurrentTier)
                .xpToNextLevel(totalNeededInTier)
                .xpRemaining(xpNextLevel - xp)
                .progressPercent(progressPercent)
                .isTopFreelancer(ul.isTopFreelancer())
                .fastResponderStreak(ul.getFastResponderStreak())
                .build();
    }

    @Override
    public List<LeaderboardEntryDTO> getLeaderboard(int topN) {
        List<UserLevel> levels = repo.findLeaderboard(PageRequest.of(0, topN));
        List<LeaderboardEntryDTO> entries = new ArrayList<>();
        
        for (int i = 0; i < levels.size(); i++) {
            UserLevel ul = levels.get(i);
            String fullName = "User " + ul.getUserId();
            
            // 🆕 Fetch name from User Microservice
            try {
                tn.esprit.gamification.client.UserClient.UserResponseDTO user = userClient.getUserById(ul.getUserId());
                if (user != null && user.getFirstName() != null) {
                    fullName = user.getFirstName() + " " + (user.getLastName() != null ? user.getLastName() : "");
                }
            } catch (Exception e) {
                // Keep default if user service is down
            }

            entries.add(LeaderboardEntryDTO.builder()
                    .rank(i + 1)
                    .userId(ul.getUserId())
                    .fullName(fullName.trim()) // 🆕
                    .xp(ul.getXp())
                    .level(ul.getLevel())
                    .isTopFreelancer(ul.isTopFreelancer())
                    .fastResponderStreak(ul.getFastResponderStreak())
                    .build());
        }
        return entries;
    }

    private int calculateLevel(int xp) {
        if (xp <= 0) return 1;
        return (int) Math.floor(Math.sqrt((double) xp / xpScaleFactor)) + 1;
    }

    @Override
    public void incrementFastResponderStreak(Long userId) {
        UserLevel ul = getUserLevel(userId);
        ul.setFastResponderStreak(ul.getFastResponderStreak() + 1);
        repo.save(ul);
    }

    @Override
    public void resetFastResponderStreak(Long userId) {
        UserLevel ul = getUserLevel(userId);
        ul.setFastResponderStreak(0);
        repo.save(ul);
    }

    @Override
    public List<UserLevel> getAllUserLevels() {
        return repo.findAll();
    }

    @Override
    public List<UserLevel> getCurrentTopFreelancers() {
        return repo.findByIsTopFreelancerTrue();
    }

    @Override
    public void setTopFreelancer(Long userId, boolean status) {
        UserLevel ul = getUserLevel(userId);
        ul.setTopFreelancer(status);
        repo.save(ul);
    }
}
