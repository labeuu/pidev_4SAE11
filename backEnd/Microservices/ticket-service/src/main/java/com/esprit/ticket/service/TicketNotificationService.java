package com.esprit.ticket.service;

import com.esprit.ticket.client.NotificationClient;
import com.esprit.ticket.dto.notification.NotificationRequestDto;
import com.esprit.ticket.entity.Ticket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Sends notifications via the Notification microservice. Failures are logged only.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketNotificationService {

    public static final String TYPE_TICKET_CREATED = "TICKET_CREATED";
    public static final String TYPE_TICKET_CLOSED = "TICKET_CLOSED";
    public static final String TYPE_TICKET_REOPENED = "TICKET_REOPENED";
    public static final String TYPE_TICKET_REPLY_ADMIN = "TICKET_REPLY_ADMIN";
    public static final String TYPE_TICKET_REPLY_USER = "TICKET_REPLY_USER";

    private final NotificationClient notificationClient;

    @Value("${app.ticket.support-inbox-user-id:}")
    private String supportInboxUserId;

    public void notifyTicketCreated(Ticket t) {
        Map<String, String> data = ticketData(t);
        notifyUser(
            t.getUserId(),
            "Support ticket opened",
            "Your ticket #" + t.getId() + " was created: " + truncate(t.getSubject(), 120),
            TYPE_TICKET_CREATED,
            data);
    }

    public void notifyTicketClosed(Ticket t) {
        notifyUser(
            t.getUserId(),
            "Ticket closed",
            "Ticket #" + t.getId() + " — \"" + truncate(t.getSubject(), 80) + "\" has been closed.",
            TYPE_TICKET_CLOSED,
            ticketData(t));
    }

    public void notifyTicketReopened(Ticket t) {
        notifyUser(
            t.getUserId(),
            "Ticket reopened",
            "Ticket #" + t.getId() + " was reopened and is waiting for support.",
            TYPE_TICKET_REOPENED,
            ticketData(t));
        if (StringUtils.hasText(supportInboxUserId)) {
            long inboxId = Long.parseLong(supportInboxUserId.trim());
            if (inboxId != t.getUserId()) {
                notifyUser(
                    inboxId,
                    "Ticket reopened",
                    "Ticket #" + t.getId() + " (\"" + truncate(t.getSubject(), 80) + "\") was reopened by the user.",
                    TYPE_TICKET_REOPENED,
                    ticketData(t));
            }
        }
    }

    public void notifyUserAboutAdminReply(Ticket t, long replyId) {
        notifyUser(
            t.getUserId(),
            "New reply on your ticket",
            "Support replied on ticket #" + t.getId() + ".",
            TYPE_TICKET_REPLY_ADMIN,
            replyData(t, replyId));
    }

    public void notifyInboxAboutUserReply(Ticket t, long replyId) {
        if (!StringUtils.hasText(supportInboxUserId)) {
            return;
        }
        notifyUser(
            Long.parseLong(supportInboxUserId.trim()),
            "New message on ticket",
            "User replied on ticket #" + t.getId() + ": \"" + truncate(t.getSubject(), 80) + "\"",
            TYPE_TICKET_REPLY_USER,
            replyData(t, replyId));
    }

    private static Map<String, String> ticketData(Ticket t) {
        Map<String, String> m = new HashMap<>();
        m.put("ticketId", String.valueOf(t.getId()));
        m.put("subject", t.getSubject() != null ? t.getSubject() : "");
        return m;
    }

    private static Map<String, String> replyData(Ticket t, long replyId) {
        Map<String, String> m = ticketData(t);
        m.put("replyId", String.valueOf(replyId));
        return m;
    }

    private void notifyUser(Long userId, String title, String body, String type, Map<String, String> data) {
        if (userId == null || title == null) {
            return;
        }
        try {
            NotificationRequestDto request = NotificationRequestDto.builder()
                .userId(String.valueOf(userId))
                .title(title)
                .body(body != null ? body : "")
                .type(type != null ? type : "GENERAL")
                .data(data)
                .build();
            notificationClient.create(request);
        } catch (Exception e) {
            log.warn("Failed to send notification to user {}: {}", userId, e.getMessage());
        }
    }

    private static String truncate(String s, int max) {
        if (s == null || s.length() <= max) {
            return s != null ? s : "";
        }
        return s.substring(0, max - 1) + "…";
    }
}
