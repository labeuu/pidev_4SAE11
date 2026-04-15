package tn.esprit.gamification.Controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import tn.esprit.gamification.Dto.LeaderboardEntryDTO;
import tn.esprit.gamification.Dto.UserLevelSummaryDTO;
import tn.esprit.gamification.Entities.UserLevel;
import tn.esprit.gamification.Services.UserLevelService;

import java.util.List;

@RestController
@RequestMapping("/api/user-level")
public class UserLevelController {

    @Autowired
    private UserLevelService service;

    @GetMapping("/{userId}")
    public UserLevel getLevel(@PathVariable Long userId) {
        return service.getUserLevel(userId);
    }

    @GetMapping("/{userId}/summary")
    public UserLevelSummaryDTO getLevelSummary(@PathVariable Long userId) {
        return service.getUserLevelSummary(userId);
    }

    @GetMapping("/leaderboard")
    public List<LeaderboardEntryDTO> getLeaderboard(@RequestParam(defaultValue = "10") int top) {
        return service.getLeaderboard(top);
    }

    @PostMapping("/{userId}/streak/update")
    public int updateStreak(@PathVariable Long userId) {
        return service.updateAndGetActiveStreak(userId);
    }
}
