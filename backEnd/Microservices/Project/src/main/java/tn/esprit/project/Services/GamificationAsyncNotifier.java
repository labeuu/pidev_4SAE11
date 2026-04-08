package tn.esprit.project.Services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tn.esprit.project.Client.GamificationClient;
import tn.esprit.project.Services.ProjectService;

@Service
@RequiredArgsConstructor
@Slf4j
public class GamificationAsyncNotifier {

    private final GamificationClient gamificationClient;

    /**
     * Ecoute l'événement de création mais UNIQUEMENT quand la base de données
     * a validé (COMMIT) l'enregistrement du projet.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notifyProjectCreated(ProjectService.ProjectCreatedEvent event) {
        Long userId = event.getClientId();
        try {
            gamificationClient.handleProjectCreated(userId);
            log.info("🎯 XP Client versé pour user {}", userId);
        } catch (Exception e) {
            log.error("❌ Échec notification Client : {}", e.getMessage());
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notifyProjectCompleted(ProjectService.ProjectCompletedEvent event) {
        Long userId = event.getFreelancerId();
        try {
            // 🛡️ Petite pause de sécurité pour laisser le temps aux index SQL du Projet
            // d'être totalement rafraîchis avant que Gamification ne vienne lire le count.
            Thread.sleep(500); 
            
            gamificationClient.handleProjectCompleted(userId); 
            log.info("🎯 XP Freelancer versé pour user {}", userId);
        } catch (Exception e) {
            log.error("❌ Échec notification Freelancer : {}", e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}
