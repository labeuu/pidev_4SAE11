package tn.esprit.freelanciajob.Service;

import java.util.Map;

/**
 * Abstraction for all outbound email operations.
 * Implementations are free to swap providers (SMTP, SendGrid, etc.)
 * without touching any business code.
 */
public interface EmailService {

    /**
     * Sends a plain-text email.
     *
     * @param to      recipient address
     * @param subject email subject
     * @param body    plain text body
     */
    void sendSimpleEmail(String to, String subject, String body);

    /**
     * Renders a Thymeleaf template and sends it as an HTML email.
     *
     * @param to           recipient address
     * @param subject      email subject
     * @param templateName path relative to {@code templates/} (e.g. "email/job-posted")
     * @param variables    variables injected into the template context
     */
    void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables);
}
