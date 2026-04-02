package tn.esprit.gamification.Controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.gamification.Entities.UserAchievement;
import tn.esprit.gamification.Services.UserAchievementService;

import java.util.List;

@RestController
@RequestMapping("/api/user-achievements")
public class UserAchievementController {

    @Autowired
    private UserAchievementService service;

    @GetMapping("/{userId}")
    public List<UserAchievement> getUserAchievements(@PathVariable Long userId) {
        return service.getUserAchievements(userId);
    }
}
