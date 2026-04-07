package com.esprit.ticket.service;

import com.esprit.ticket.domain.TicketPriority;
import com.esprit.ticket.domain.TicketStatus;
import com.esprit.ticket.dto.ticket.CreateTicketRequest;
import com.esprit.ticket.dto.ticket.TicketResponse;
import com.esprit.ticket.dto.ticket.UpdateTicketRequest;
import com.esprit.ticket.entity.Ticket;
import com.esprit.ticket.exception.EntityNotFoundException;
import com.esprit.ticket.repository.TicketRepository;
import com.esprit.ticket.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.http.HttpStatus.*;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final CurrentUserService currentUserService;
    private final ContentModerationService contentModerationService;

    @Value("${app.ticket.auto-close-after-hours:48}")
    private long autoCloseAfterHours;

    @Transactional
    public TicketResponse create(CreateTicketRequest req) {
        Long currentUserId = currentUserService.requireCurrentUserId();
        String subject = contentModerationService.censorIfProfane(req.subject()).trim();
        TicketPriority priority = smartPriority(subject);

        Ticket t = Ticket.builder()
                .userId(currentUserId)
                .subject(subject)
                .status(TicketStatus.OPEN)
                .priority(priority)
                .lastActivityAt(LocalDateTime.now())
                .build();
        t = ticketRepository.save(t);
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
            String subject = contentModerationService.censorIfProfane(req.subject().trim());
            if (!subject.isBlank()) {
                t.setSubject(subject);
                // Re-evaluate priority only if priority is not explicitly set in this update
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
        t.setLastActivityAt(LocalDateTime.now());
        return toResponse(ticketRepository.save(t));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void delete(Long id) {
        if (!ticketRepository.existsById(id)) throw new EntityNotFoundException("Ticket", id);
        ticketRepository.deleteById(id);
    }

    @Transactional
    public int autoCloseInactiveOpenTickets() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(autoCloseAfterHours);
        List<Ticket> stale = ticketRepository.findByStatusAndLastActivityAtBefore(TicketStatus.OPEN, cutoff);
        for (Ticket t : stale) {
            t.setStatus(TicketStatus.CLOSED);
        }
        ticketRepository.saveAll(stale);
        return stale.size();
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
        // Keep it simple and predictable: default MEDIUM.
        return TicketPriority.MEDIUM;
    }

    private TicketResponse toResponse(Ticket t) {
        return new TicketResponse(
                t.getId(),
                t.getUserId(),
                t.getSubject(),
                t.getStatus(),
                t.getPriority(),
                t.getCreatedAt(),
                t.getLastActivityAt()
        );
    }
}

