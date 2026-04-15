package org.example.subcontracting.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.subcontracting.client.UserFeignClient;
import org.example.subcontracting.client.dto.UserRemoteDto;
import org.example.subcontracting.dto.response.PredictiveDashboardResponse;
import org.example.subcontracting.entity.Subcontract;
import org.example.subcontracting.entity.SubcontractMediaType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubcontractEmailService {

    private final JavaMailSender mailSender;
    @Qualifier("mailTemplateEngine")
    private final TemplateEngine mailTemplateEngine;
    private final UserFeignClient userFeignClient;
    private final ObjectMapper objectMapper;

    @Value("${app.mail.from:noreply@smart-freelance.local}")
    private String fromEmail;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    @Value("${app.mail.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    private static final DateTimeFormatter DATE_FR = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);

    @Async
    public void sendProposedEmail(Subcontract sc) {
        if (!mailEnabled) {
            log.debug("[MAIL] Désactivé — sous-traitance {}", sc.getId());
            return;
        }
        String toEmail;
        String subName;
        try {
            UserRemoteDto sub = userFeignClient.getUserById(sc.getSubcontractorId());
            if (sub == null || sub.getEmail() == null || sub.getEmail().isBlank()) {
                log.warn("[MAIL] Pas d'e-mail pour sous-traitant userId={}", sc.getSubcontractorId());
                return;
            }
            toEmail = sub.getEmail().trim();
            subName = ((sub.getFirstName() != null ? sub.getFirstName() : "") + " "
                    + (sub.getLastName() != null ? sub.getLastName() : "")).trim();
            if (subName.isBlank()) subName = "Bonjour";
        } catch (Exception e) {
            log.warn("[MAIL] User service indisponible pour sous-traitance {}: {}", sc.getId(), e.getMessage());
            return;
        }

        String mainName = "Freelancer principal";
        try {
            UserRemoteDto main = userFeignClient.getUserById(sc.getMainFreelancerId());
            if (main != null) {
                String mn = ((main.getFirstName() != null ? main.getFirstName() : "") + " "
                        + (main.getLastName() != null ? main.getLastName() : "")).trim();
                if (!mn.isBlank()) mainName = mn;
            }
        } catch (Exception ignored) { }

        List<String> skills = parseSkills(sc.getRequiredSkillsJson());

        Context ctx = new Context(Locale.FRENCH);
        ctx.setVariable("subcontractorName", subName);
        ctx.setVariable("mainFreelancerName", mainName);
        ctx.setVariable("title", sc.getTitle() != null ? sc.getTitle() : "");
        ctx.setVariable("scope", sc.getScope() != null ? sc.getScope() : "—");
        ctx.setVariable("budgetFormatted", formatBudget(sc.getBudget(), sc.getCurrency()));
        ctx.setVariable("startDate", sc.getStartDate() != null ? DATE_FR.format(sc.getStartDate()) : "—");
        ctx.setVariable("deadline", sc.getDeadline() != null ? DATE_FR.format(sc.getDeadline()) : "—");
        ctx.setVariable("skills", skills);
        ctx.setVariable("hasSkills", !skills.isEmpty());

        boolean hasMedia = sc.getMediaUrl() != null && !sc.getMediaUrl().isBlank();
        ctx.setVariable("hasMedia", hasMedia);
        ctx.setVariable("mediaUrl", hasMedia ? sc.getMediaUrl().trim() : "");
        ctx.setVariable("mediaIsVideo", sc.getMediaType() == SubcontractMediaType.VIDEO);
        ctx.setVariable("mediaIsAudio", sc.getMediaType() == SubcontractMediaType.AUDIO);
        ctx.setVariable("frontendUrl", frontendUrl);
        ctx.setVariable("subcontractorDashboardUrl", frontendUrl.replaceAll("/$", "") + "/dashboard/subcontractor-work");

        try {
            String html = mailTemplateEngine.process("mail/subcontract-proposed", ctx);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("[Smart Freelance] Proposition de sous-traitance : " + sc.getTitle());
            helper.setText(html, true);
            mailSender.send(message);
            log.info("[MAIL] E-mail PROPOSED envoyé à {} pour sous-traitance {}", toEmail, sc.getId());
        } catch (Exception e) {
            log.error("[MAIL] Échec envoi sous-traitance {}: {}", sc.getId(), e.getMessage());
        }
    }

    private List<String> parseSkills(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String formatBudget(BigDecimal budget, String currency) {
        if (budget == null) return "—";
        String cur = currency != null ? currency : "TND";
        return budget.stripTrailingZeros().toPlainString() + " " + cur;
    }

    @Async
    public void sendMonthlyPerformanceReport(Long mainFreelancerId, PredictiveDashboardResponse dashboard) {
        if (!mailEnabled) return;
        try {
            UserRemoteDto main = userFeignClient.getUserById(mainFreelancerId);
            if (main == null || main.getEmail() == null || main.getEmail().isBlank()) return;
            String name = ((main.getFirstName() != null ? main.getFirstName() : "") + " "
                    + (main.getLastName() != null ? main.getLastName() : "")).trim();
            if (name.isBlank()) name = "Freelancer";

            Context ctx = new Context(Locale.FRENCH);
            ctx.setVariable("name", name);
            ctx.setVariable("summary", dashboard.getNarrativeSummary());
            ctx.setVariable("nextIncident", dashboard.getNextIncidentPrediction());
            ctx.setVariable("successByCategory", dashboard.getSuccessRateByCategory());
            ctx.setVariable("bestMonths", dashboard.getBestMonthsForSubcontracting());
            ctx.setVariable("generatedAt", dashboard.getGeneratedAt());
            ctx.setVariable("dashboardUrl", frontendUrl.replaceAll("/$", "") + "/dashboard/my-subcontracts");

            String html = mailTemplateEngine.process("mail/subcontract-monthly-report", ctx);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(main.getEmail().trim());
            helper.setSubject("[Smart Freelance] Rapport mensuel sous-traitance");
            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("[MAIL] Rapport mensuel non envoyé freelancer={}: {}", mainFreelancerId, e.getMessage());
        }
    }

    @Async
    public void sendChatMessageEmail(String toEmail, String recipientName, String senderName,
                                     String subcontractTitle, String messageText) {
        if (!mailEnabled) return;
        if (toEmail == null || toEmail.isBlank()) return;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("[Smart Freelance] Nouveau message de sous-traitance");

            String safeRecipient = (recipientName == null || recipientName.isBlank()) ? "Freelancer" : recipientName;
            String safeSender = (senderName == null || senderName.isBlank()) ? "Votre interlocuteur" : senderName;
            String safeTitle = (subcontractTitle == null || subcontractTitle.isBlank()) ? "Mission de sous-traitance" : subcontractTitle;
            String preview = messageText == null ? "" : (messageText.length() > 240 ? messageText.substring(0, 240) + "…" : messageText);

            String html = """
                    <!DOCTYPE html>
                    <html lang="fr">
                    <head><meta charset="UTF-8"></head>
                    <body style="font-family: Arial, sans-serif; background: #f4f4f4; margin: 0; padding: 20px;">
                      <div style="max-width: 600px; margin: auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.08);">
                        <div style="background: linear-gradient(135deg, #2563eb, #0ea5e9); padding: 24px 28px;">
                          <h1 style="color: #ffffff; margin: 0; font-size: 21px;">Nouveau message</h1>
                          <p style="color: rgba(255,255,255,0.9); margin: 8px 0 0;">Conversation de sous-traitance</p>
                        </div>
                        <div style="padding: 24px 28px;">
                          <p style="color: #334155;">Bonjour <strong>%s</strong>,</p>
                          <p style="color: #334155;"><strong>%s</strong> vous a envoyé un message pour la mission <strong>%s</strong>.</p>
                          <div style="background: #f8fafc; border-left: 4px solid #2563eb; border-radius: 0 8px 8px 0; padding: 12px 14px; margin: 16px 0;">
                            <p style="margin: 0; color: #0f172a; font-style: italic;">"%s"</p>
                          </div>
                          <div style="text-align: center; margin-top: 18px;">
                            <a href="%s/dashboard/subcontractor-work"
                               style="background: #2563eb; color: #ffffff; text-decoration: none; padding: 11px 20px; border-radius: 8px; font-weight: 700; display: inline-block;">
                              Ouvrir la conversation
                            </a>
                          </div>
                        </div>
                      </div>
                    </body>
                    </html>
                    """.formatted(
                    safeRecipient,
                    safeSender,
                    safeTitle,
                    preview,
                    frontendUrl.replaceAll("/$", "")
            );

            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("[MAIL] Échec e-mail chat sous-traitance to={}: {}", toEmail, e.getMessage());
        }
    }
}
