package tn.esprit.gamification.Controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.gamification.Entities.UserLevel;
import tn.esprit.gamification.Services.UserLevelService;

@RestController
@RequestMapping("/api/user-level")
public class UserLevelController {

    @Autowired
    private UserLevelService service;

    @GetMapping("/{userId}")
    public UserLevel getLevel(@PathVariable Long userId) {
        return service.getUserLevel(userId);
    }
}
