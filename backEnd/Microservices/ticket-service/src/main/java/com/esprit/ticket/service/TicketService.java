package com.esprit.ticket.service;

import com.esprit.ticket.TicketConstants;
import com.esprit.ticket.domain.ReplySender;
import com.esprit.ticket.domain.TicketPriority;
import com.esprit.ticket.domain.TicketStatus;
import com.esprit.ticket.dto.ticket.CreateTicketRequest;
import com.esprit.ticket.dto.ticket.MonthlyTicketCount;
import com.esprit.ticket.dto.ticket.TicketResponse;
import com.esprit.ticket.dto.ticket.TicketStatsResponse;
import com.esprit.ticket.dto.ticket.TicketUnreadCountEntry;
import com.esprit.ticket.dto.ticket.UpdateTicketRequest;
import com.esprit.ticket.entity.Ticket;
import com.esprit.ticket.entity.TicketReply;
import com.esprit.ticket.exception.EntityNotFoundException;
import com.esprit.ticket.repository.TicketReplyRepository;
import com.esprit.ticket.repository.TicketRepository;
import com.esprit.ticket.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpStatus.*;

@Service
@RequiredArgsConstructor
public class TicketService {

    private static final int SPAM_WINDOW_MINUTES = 10;
    private static final int SPAM_MAX_TICKETS = 5;

    private final TicketRepository ticketRepository;
    private final TicketReplyRepository ticketReplyRepository;
    private final CurrentUserService currentUserService;
    private final ContentModerationService contentModerationService;
    private final ReplyService replyService;

    @Value("${app.ticket.auto-close-after-hours:48}")
    private long autoCloseAfterHours;

    @Transactional
    public TicketResponse create(CreateTicketRequest req) {
        Long currentUserId = currentUserService.requireCurrentUserId();
        LocalDateTime since = LocalDateTime.now().minusMinutes(SPAM_WINDOW_MINUTES);
        if (ticketRepository.countByUserIdAndCreatedAtAfter(currentUserId, since) >= SPAM_MAX_TICKETS) {
            throw new ResponseStatusException(
                    TOO_MANY_REQUESTS, "Too many tickets created in a short period. Please wait before opening another.");
        }

        String subject = contentModerationService.validateAndPrepareText(req.subject()).trim();
        if (subject.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Subject is required");
        }
        TicketPriority priority = smartPriority(subject);

        Ticket t = Ticket.builder()
                .userId(currentUserId)
                .subject(subject)
                .status(TicketStatus.OPEN)
                .priority(priority)
                .lastActivityAt(LocalDateTime.now())
                .reopenCount(0)
                .build();
        t = ticketRepository.save(t);

        TicketReply welcome = TicketReply.builder()
                .ticket(t)
                .message(TicketConstants.WELCOME_REPLY_MESSAGE)
                .sender(ReplySender.ADMIN)
                .authorUserId(TicketConstants.SYSTEM_AUTHOR_USER_ID)
                .readByUser(false)
                .readByAdmin(true)
                .build();
        ticketReplyRepository.save(welcome);

        return toResponse(t);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<TicketResponse> getAll() {
        return ticketRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TicketResponse getById(Long id) {
        Ticket t = ticketRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Ticket", id));
        enforceTicketReadableByCaller(t);
        return toResponse(t);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> getByUserId(Long userId) {
        Long currentUserId = currentUserService.requireCurrentUserId();
        if (!currentUserService.isAdmin() && !currentUserId.equals(userId)) {
            throw new ResponseStatusException(FORBIDDEN, "Not allowed");
        }
        return ticketRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public TicketResponse update(Long id, UpdateTicketRequest req) {
        Ticket t = ticketRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Ticket", id));
        enforceTicketOwnedOrAdmin(t);
        if (t.getStatus() == TicketStatus.CLOSED) {
            throw new ResponseStatusException(CONFLICT, "Ticket is CLOSED");
        }
        boolean changed = false;
        if (req.subject() != null) {
            String subject = contentModerationService.validateAndPrepareText(req.subject().trim());
            if (!subject.isBlank()) {
                t.setSubject(subject);
                if (req.priority() == null) {
                    t.setPriority(smartPriority(subject));
                }
                changed = true;
            }
        }
        if (req.priority() != null) {
            t.setPriority(req.priority());
            changed = true;
        }
        if (!changed) {
            throw new ResponseStatusException(BAD_REQUEST, "No fields to update");
        }
        t.setLastActivityAt(LocalDateTime.now());
        return toResponse(ticketRepository.save(t));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public TicketResponse close(Long id) {
        Ticket t = ticketRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Ticket", id));
        if (t.getStatus() == TicketStatus.CLOSED) return toResponse(t);
        t.setStatus(TicketStatus.CLOSED);
        t.setResolvedAt(LocalDateTime.now());
        t.setLastActivityAt(LocalDateTime.now());
        return toResponse(ticketRepository.save(t));
    }

    @Transactional
    public TicketResponse reopen(Long id) {
        Ticket t = ticketRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Ticket", id));
        Long currentUserId = currentUserService.requireCurrentUserId();
        if (!currentUserService.isAdmin() && !currentUserId.equals(t.getUserId())) {
            throw new ResponseStatusException(FORBIDDEN, "Only the ticket owner or an admin can reopen");
        }
        if (t.getStatus() != TicketStatus.CLOSED) {
            throw new ResponseStatusException(CONFLICT, "Ticket is not closed");
        }
        if (t.getReopenCount() >= 1) {
            throw new ResponseStatusException(CONFLICT, "This ticket can only be reopened once");
        }
        t.setStatus(TicketStatus.OPEN);
        t.setReopenCount(t.getReopenCount() + 1);
        t.setLastActivityAt(LocalDateTime.now());
        return toResponse(ticketRepository.save(t));
    }

    @Transactional
    public void markTicketRepliesRead(Long ticketId) {
        Ticket t = ticketRepository.findById(ticketId).orElseThrow(() -> new EntityNotFoundException("Ticket", ticketId));
        enforceTicketReadableByCaller(t);
        replyService.markRepliesReadForViewer(ticketId);
    }

    @Transactional(readOnly = true)
    public List<TicketUnreadCountEntry> unreadCountsForCurrentUser() {
        if (currentUserService.isAdmin()) {
            return replyService.unreadUserMessageCountsByTicketForAdmin();
        }
        Long uid = currentUserService.requireCurrentUserId();
        return replyService.unreadAdminMessageCountsByTicketForUser(uid);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void delete(Long id) {
        if (!ticketRepository.existsById(id)) throw new EntityNotFoundException("Ticket", id);
        ticketReplyRepository.deleteByTicket_Id(id);
        ticketRepository.deleteById(id);
    }

    @Transactional
    public int autoCloseInactiveOpenTickets() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(autoCloseAfterHours);
        List<Ticket> stale = ticketRepository.findByStatusAndLastActivityAtBefore(TicketStatus.OPEN, cutoff);
        LocalDateTime now = LocalDateTime.now();
        for (Ticket t : stale) {
            t.setStatus(TicketStatus.CLOSED);
            t.setResolvedAt(now);
        }
        ticketRepository.saveAll(stale);
        return stale.size();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public TicketStatsResponse getStats() {
        long total = ticketRepository.count();
        long open = ticketRepository.countByStatus(TicketStatus.OPEN);
        long closed = ticketRepository.countByStatus(TicketStatus.CLOSED);
        Double avg = ticketRepository.averageResponseTimeMinutes();
        return new TicketStatsResponse(total, open, closed, avg);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<MonthlyTicketCount> getMonthlyStats() {
        List<Object[]> rows = ticketRepository.countTicketsGroupedByYearMonth();
        List<MonthlyTicketCount> out = new ArrayList<>();
        for (Object[] row : rows) {
            int year = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            long count = ((Number) row[2]).longValue();
            out.add(new MonthlyTicketCount(year, month, count));
        }
        return out;
    }

    private void enforceTicketReadableByCaller(Ticket t) {
        if (currentUserService.isAdmin()) return;
        Long currentUserId = currentUserService.requireCurrentUserId();
        if (!currentUserId.equals(t.getUserId())) {
            throw new ResponseStatusException(FORBIDDEN, "Not allowed");
        }
    }

    private void enforceTicketOwnedOrAdmin(Ticket t) {
        if (currentUserService.isAdmin()) return;
        Long currentUserId = currentUserService.requireCurrentUserId();
        if (!currentUserId.equals(t.getUserId())) {
            throw new ResponseStatusException(FORBIDDEN, "Not allowed");
        }
    }

    private TicketPriority smartPriority(String subject) {
        if (subject == null) return TicketPriority.MEDIUM;
        String s = subject.toLowerCase();
        if (s.contains("urgent") || s.contains("payment") || s.contains("error")) {
            return TicketPriority.HIGH;
        }
        return TicketPriority.MEDIUM;
    }

    private TicketResponse toResponse(Ticket t) {
        boolean canReopen = t.getStatus() == TicketStatus.CLOSED && t.getReopenCount() < 1;
        return new TicketResponse(
                t.getId(),
                t.getUserId(),
                t.getSubject(),
                t.getStatus(),
                t.getPriority(),
                t.getCreatedAt(),
                t.getLastActivityAt(),
                t.getFirstResponseAt(),
                t.getResolvedAt(),
                t.getResponseTimeMinutes(),
                t.getReopenCount(),
                canReopen);
    }
}
