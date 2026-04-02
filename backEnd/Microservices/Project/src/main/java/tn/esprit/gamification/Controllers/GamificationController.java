package tn.esprit.gamification.Controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.gamification.Scheduler.FastResponderScheduler;
import tn.esprit.gamification.Scheduler.TopFreelancerScheduler;
import tn.esprit.gamification.Services.GamificationService;

@RestController
@RequestMapping("/api/gamification")
public class GamificationController {

    @Autowired
    private GamificationService service;
    @Autowired
    private TopFreelancerScheduler topFreelancerScheduler;

    @Autowired
    private FastResponderScheduler fastResponderScheduler;

    @PostMapping("/project-completed")
    public void projectCompleted(@RequestParam Long userId) {
        service.handleProjectCompleted(userId);
    }

    @PostMapping("/project-created")
    public void projectCreated(@RequestParam Long userId) {
        service.handleProjectCreated(userId);
    }
    // 🆕 Simuler une réponse rapide d'un freelancer
    @PostMapping("/fast-response")
    public void fastResponse(@RequestParam Long userId) {
        service.handleFastResponse(userId);
    }

    // 🆕 Déclencher manuellement les schedulers (pour tests)
    @PostMapping("/trigger/top-freelancer")
    public String triggerTopFreelancer() {
        topFreelancerScheduler.detectAndRewardTopFreelancer();
        return "✅ Top Freelancer détecté et récompensé";
    }

    @PostMapping("/trigger/fast-responder")
    public String triggerFastResponder() {
        fastResponderScheduler.checkFastResponders();
        return "✅ Fast Responders vérifiés";
    }
}
