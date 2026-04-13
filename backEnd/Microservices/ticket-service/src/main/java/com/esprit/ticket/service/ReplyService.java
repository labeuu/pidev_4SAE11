package com.esprit.ticket.service;

import com.esprit.ticket.TicketConstants;
import com.esprit.ticket.domain.ReplySender;
import com.esprit.ticket.domain.TicketPriority;
import com.esprit.ticket.domain.TicketStatus;
import com.esprit.ticket.dto.reply.CreateReplyRequest;
import com.esprit.ticket.dto.reply.ReplyResponse;
import com.esprit.ticket.dto.reply.UpdateReplyRequest;
import com.esprit.ticket.dto.ticket.TicketUnreadCountEntry;
import com.esprit.ticket.entity.Ticket;
import com.esprit.ticket.entity.TicketReply;
import com.esprit.ticket.exception.EntityNotFoundException;
import com.esprit.ticket.repository.TicketReplyRepository;
import com.esprit.ticket.repository.TicketRepository;
import com.esprit.ticket.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpStatus.*;

@Service
@RequiredArgsConstructor
public class ReplyService {

    private static final long SLA_RESPONSE_MINUTES = 120L;

    private final TicketRepository ticketRepository;
    private final TicketReplyRepository replyRepository;
    private final CurrentUserService currentUserService;
    private final ContentModerationService contentModerationService;

    @Transactional
    public ReplyResponse addReply(CreateReplyRequest req) {
        Ticket t = ticketRepository
                .findById(req.ticketId())
                .orElseThrow(() -> new EntityNotFoundException("Ticket", req.ticketId()));

        if (!currentUserService.isAdmin()) {
            Long uid = currentUserService.requireCurrentUserId();
            if (!uid.equals(t.getUserId())) {
                throw new ResponseStatusException(FORBIDDEN, "Not allowed");
            }
        }
        if (t.getStatus() == TicketStatus.CLOSED) {
            throw new ResponseStatusException(CONFLICT, "Ticket is CLOSED");
        }

        Long authorUserId = currentUserService.requireCurrentUserId();
        ReplySender sender = currentUserService.isAdmin() ? ReplySender.ADMIN : ReplySender.USER;
        String message = contentModerationService.validateAndPrepareText(req.message()).trim();
        if (message.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Message is required");
        }

        boolean readByUser = sender == ReplySender.USER;
        boolean readByAdmin = sender == ReplySender.ADMIN;

        LocalDateTime now = LocalDateTime.now();
        if (sender == ReplySender.ADMIN
                && authorUserId != TicketConstants.SYSTEM_AUTHOR_USER_ID
                && t.getFirstResponseAt() == null) {
            t.setFirstResponseAt(now);
            long minutes = ChronoUnit.MINUTES.between(t.getCreatedAt(), now);
            t.setResponseTimeMinutes(minutes);
            if (minutes > SLA_RESPONSE_MINUTES) {
                t.setPriority(TicketPriority.HIGH);
            }
        }

        TicketReply r = TicketReply.builder()
                .ticket(t)
                .message(message)
                .sender(sender)
                .authorUserId(authorUserId)
                .readByUser(readByUser)
                .readByAdmin(readByAdmin)
                .build();
        r = replyRepository.save(r);

        t.setLastActivityAt(now);
        ticketRepository.save(t);

        return toResponse(r);
    }

    @Transactional(readOnly = true)
    public List<ReplyResponse> getByTicketId(Long ticketId) {
        Ticket t = ticketRepository.findById(ticketId).orElseThrow(() -> new EntityNotFoundException("Ticket", ticketId));
        if (!currentUserService.isAdmin()) {
            Long uid = currentUserService.requireCurrentUserId();
            if (!uid.equals(t.getUserId())) {
                throw new ResponseStatusException(FORBIDDEN, "Not allowed");
            }
        }
        return replyRepository.findByTicket_IdOrderByCreatedAtAsc(ticketId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void markRepliesReadForViewer(Long ticketId) {
        if (currentUserService.isAdmin()) {
            replyRepository.markReadByAdminForSender(ticketId, ReplySender.USER);
        } else {
            replyRepository.markReadByUserForSender(ticketId, ReplySender.ADMIN);
        }
    }

    @Transactional(readOnly = true)
    public List<TicketUnreadCountEntry> unreadAdminMessageCountsByTicketForUser(Long userId) {
        List<Object[]> rows =
                replyRepository.countUnreadAdminRepliesByTicketForUser(userId, ReplySender.ADMIN);
        return mapUnreadRows(rows);
    }

    @Transactional(readOnly = true)
    public List<TicketUnreadCountEntry> unreadUserMessageCountsByTicketForAdmin() {
        List<Object[]> rows = replyRepository.countUnreadUserRepliesByTicketForAdmin(ReplySender.USER);
        return mapUnreadRows(rows);
    }

    private static List<TicketUnreadCountEntry> mapUnreadRows(List<Object[]> rows) {
        List<TicketUnreadCountEntry> out = new ArrayList<>();
        for (Object[] r : rows) {
            out.add(new TicketUnreadCountEntry(((Number) r[0]).longValue(), ((Number) r[1]).longValue()));
        }
        return out;
    }

    @Transactional
    public ReplyResponse updateReply(Long replyId, UpdateReplyRequest req) {
        TicketReply r = replyRepository.findById(replyId).orElseThrow(() -> new EntityNotFoundException("TicketReply", replyId));
        Ticket t = r.getTicket();

        if (t.getStatus() == TicketStatus.CLOSED) {
            throw new ResponseStatusException(CONFLICT, "Ticket is CLOSED");
        }

        Long uid = currentUserService.requireCurrentUserId();
        if (!uid.equals(r.getAuthorUserId())) {
            throw new ResponseStatusException(FORBIDDEN, "Only author can modify");
        }

        String msg = contentModerationService.validateAndPrepareText(req.message()).trim();
        if (msg.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Message is required");
        }
        r.setMessage(msg);
        r = replyRepository.save(r);

        t.setLastActivityAt(LocalDateTime.now());
        ticketRepository.save(t);

        return toResponse(r);
    }

    @Transactional
    public void deleteReply(Long replyId) {
        TicketReply r = replyRepository.findById(replyId).orElseThrow(() -> new EntityNotFoundException("TicketReply", replyId));

        Long uid = currentUserService.requireCurrentUserId();
        boolean isAuthor = uid.equals(r.getAuthorUserId());
        if (!isAuthor && !currentUserService.isAdmin()) {
            throw new ResponseStatusException(FORBIDDEN, "Not allowed");
        }

        replyRepository.deleteById(replyId);
    }

    private ReplyResponse toResponse(TicketReply r) {
        return new ReplyResponse(
                r.getId(),
                r.getTicket().getId(),
                r.getMessage(),
                r.getSender(),
                r.getAuthorUserId(),
                r.getCreatedAt(),
                r.isReadByUser(),
                r.isReadByAdmin());
    }
}
