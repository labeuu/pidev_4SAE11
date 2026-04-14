package tn.esprit.freelanciajob.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

/**
 * SMTP-backed implementation of {@link EmailService}.
 *
 * Both methods are {@code @Async} so they never block the calling thread.
 * All failures are caught and logged — email errors must not roll back
 * a business transaction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Value("${app.mail.from-name:Freelancia Platform}")
    private String fromName;

    // ─── Plain text ──────────────────────────────────────────────────────────

    @Async("emailTaskExecutor")
    @Override
    public void sendSimpleEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(String.format("%s <%s>", fromName, fromAddress));
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("[Email] Simple email sent → {}", to);
        } catch (MailException ex) {
            log.error("[Email] Failed to send simple email to {}: {}", to, ex.getMessage());
        }
    }

    // ─── HTML / Thymeleaf ────────────────────────────────────────────────────

    @Async("emailTaskExecutor")
    @Override
    public void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        try {
            // 1. Render the template
            Context ctx = new Context();
            ctx.setVariables(variables);
            String html = templateEngine.process(templateName, ctx);

            // 2. Build a MIME message
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(String.format("%s <%s>", fromName, fromAddress));
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true); // true = isHtml

            mailSender.send(mimeMessage);
            log.info("[Email] HTML email sent → {} (template: {})", to, templateName);

        } catch (MailException ex) {
            log.error("[Email] MailException while sending HTML email to {}: {}", to, ex.getMessage());
        } catch (MessagingException ex) {
            log.error("[Email] MessagingException while sending HTML email to {}: {}", to, ex.getMessage());
        }
    }
}
