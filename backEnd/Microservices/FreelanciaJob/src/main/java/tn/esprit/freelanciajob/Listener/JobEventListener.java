package tn.esprit.freelanciajob.Listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import tn.esprit.freelanciajob.Client.NotificationClient;
import tn.esprit.freelanciajob.Client.UserClient;
import tn.esprit.freelanciajob.Dto.request.NotificationCreateRequest;
import tn.esprit.freelanciajob.Dto.response.UserDto;
import tn.esprit.freelanciajob.Entity.Job;
import tn.esprit.freelanciajob.Entity.JobApplication;
import tn.esprit.freelanciajob.Event.ApplicationAcceptedEvent;
import tn.esprit.freelanciajob.Event.ApplicationSubmittedEvent;
import tn.esprit.freelanciajob.Event.JobCreatedEvent;
import tn.esprit.freelanciajob.Service.EmailService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Listens for domain events: outbound HTML email (SMTP) and in-app notifications (Notification service).
 *
 * Keeps {@link tn.esprit.freelanciajob.Service.JobApplicationServiceImpl} free of integration details.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobEventListener {

    private final EmailService emailService;
    private final UserClient userClient;
    private final NotificationClient notificationClient;

    // ─── A. New job posted → notify all freelancers ───────────────────────────

    @Async("emailTaskExecutor")
    @EventListener
    public void onJobCreated(JobCreatedEvent event) {
        Job job = event.getJob();
        String clientName = event.getClientName();

        List<UserDto> freelancers = userClient.getUsersByRole("FREELANCER");
        if (freelancers.isEmpty()) {
            log.warn("[JobEventListener] No freelancers found or USER service unavailable.");
            return;
        }

        String subject = "New Job Posted: " + job.getTitle();

        for (UserDto freelancer : freelancers) {
            if (freelancer.getEmail() == null) continue;

            Map<String, Object> vars = new HashMap<>();
            vars.put("freelancerName", freelancer.getFirstName());
            vars.put("jobTitle", job.getTitle());
            vars.put("clientName", clientName);
            vars.put("jobCategory", job.getCategory());
            vars.put("jobLocation", job.getLocationType() != null ? job.getLocationType().name() : "N/A");
            vars.put("budgetMin", job.getBudgetMin());
            vars.put("budgetMax", job.getBudgetMax());
            vars.put("currency", job.getCurrency());

            emailService.sendHtmlEmail(freelancer.getEmail(), subject, "email/job-posted", vars);
        }

        log.info("[JobEventListener] 'Job Created' emails queued for {} freelancers (job: {})",
                freelancers.size(), job.getTitle());
    }

    // ─── B. Application submitted → confirm to freelancer + notify client ───

    @Async("emailTaskExecutor")
    @EventListener
    public void onApplicationSubmitted(ApplicationSubmittedEvent event) {
        JobApplication application = event.getApplication();
        Long freelancerId = event.getFreelancerId();
        Job job = application.getJob();
        String jobTitle = job.getTitle();

        // B1 – confirmation to the freelancer
        UserDto freelancer = userClient.getUserById(freelancerId);
        if (freelancer == null || freelancer.getEmail() == null) {
            log.warn("[JobEventListener] Cannot send submission email: user {} not found.", freelancerId);
        } else {
            Map<String, Object> vars = new HashMap<>();
            vars.put("freelancerName", freelancer.getFirstName());
            vars.put("jobTitle", jobTitle);
            vars.put("applicationId", application.getId());
            emailService.sendHtmlEmail(
                    freelancer.getEmail(),
                    "Application Submitted Successfully - " + jobTitle,
                    "email/application-submitted", vars);
            log.info("[JobEventListener] Application-submitted email sent to user {}", freelancerId);
        }

        // B2 – notification to the client who owns the job
        if (job.getClientId() != null) {
            UserDto client = userClient.getUserById(job.getClientId());
            if (client != null && client.getEmail() != null) {
                Map<String, Object> vars = new HashMap<>();
                vars.put("clientName", client.getFirstName());
                vars.put("jobTitle", jobTitle);
                vars.put("freelancerName",
                        freelancer != null ? freelancer.getFirstName() : "A freelancer");
                vars.put("applicationId", application.getId());
                emailService.sendHtmlEmail(
                        client.getEmail(),
                        "New Application Received for: " + jobTitle,
                        "email/client-application-received", vars);
                log.info("[JobEventListener] Client-notification email sent to user {}", job.getClientId());
            }
        }

        // B3 – in-app notification for the client (Notification microservice / Firestore)
        if (job.getClientId() != null) {
            String applicantLabel = freelancer != null && freelancer.getFirstName() != null
                    ? freelancer.getFirstName()
                    : "A freelancer";
            try {
                notificationClient.create(NotificationCreateRequest.builder()
                        .userId(String.valueOf(job.getClientId()))
                        .title("New application · " + jobTitle)
                        .body(applicantLabel + " applied to your job \"" + jobTitle + "\".")
                        .type("JOB_APPLICATION")
                        .data(Map.of(
                                "jobId", String.valueOf(job.getId()),
                                "applicationId", String.valueOf(application.getId())
                        ))
                        .build());
                log.info("[JobEventListener] In-app notification created for client user {}", job.getClientId());
            } catch (Exception ex) {
                log.warn("[JobEventListener] In-app notification failed (email was still sent): {}", ex.getMessage());
            }
        }
    }

    // ─── C. Application accepted → congratulate freelancer ───────────────────

    @Async("emailTaskExecutor")
    @EventListener
    public void onApplicationAccepted(ApplicationAcceptedEvent event) {
        JobApplication application = event.getApplication();
        Long freelancerId = event.getFreelancerId();

        UserDto freelancer = userClient.getUserById(freelancerId);
        if (freelancer == null || freelancer.getEmail() == null) {
            log.warn("[JobEventListener] Cannot send acceptance email: user {} not found.", freelancerId);
            return;
        }

        String jobTitle = application.getJob().getTitle();
        String subject = "Application Accepted: Next Steps for " + jobTitle;

        Map<String, Object> vars = new HashMap<>();
        vars.put("freelancerName", freelancer.getFirstName());
        vars.put("jobTitle", jobTitle);
        vars.put("applicationId", application.getId());

        emailService.sendHtmlEmail(freelancer.getEmail(), subject, "email/application-accepted", vars);
        log.info("[JobEventListener] Application-accepted email sent to user {}", freelancerId);
    }
}
