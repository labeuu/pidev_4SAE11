package com.esprit.ticket.service;

import com.esprit.ticket.domain.ReplySender;
import com.esprit.ticket.domain.TicketStatus;
import com.esprit.ticket.dto.reply.CreateReplyRequest;
import com.esprit.ticket.dto.reply.ReplyResponse;
import com.esprit.ticket.dto.reply.UpdateReplyRequest;
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
import java.util.List;

import static org.springframework.http.HttpStatus.*;

@Service
@RequiredArgsConstructor
public class ReplyService {

    private final TicketRepository ticketRepository;
    private final TicketReplyRepository replyRepository;
    private final CurrentUserService currentUserService;
    private final ContentModerationService contentModerationService;

    @Transactional
    public ReplyResponse addReply(CreateReplyRequest req) {
        Ticket t = ticketRepository.findById(req.ticketId())
                .orElseThrow(() -> new EntityNotFoundException("Ticket", req.ticketId()));

        // Ticket access: admin can reply to any; user can reply to own ticket
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
        String message = contentModerationService.censorIfProfane(req.message()).trim();

        TicketReply r = TicketReply.builder()
                .ticket(t)
                .message(message)
                .sender(sender)
                .authorUserId(authorUserId)
                .build();
        r = replyRepository.save(r);

        t.setLastActivityAt(LocalDateTime.now());
        ticketRepository.save(t);

        return toResponse(r);
    }

    @Transactional(readOnly = true)
    public List<ReplyResponse> getByTicketId(Long ticketId) {
        Ticket t = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket", ticketId));
        // ticket access
        if (!currentUserService.isAdmin()) {
            Long uid = currentUserService.requireCurrentUserId();
            if (!uid.equals(t.getUserId())) {
                throw new ResponseStatusException(FORBIDDEN, "Not allowed");
            }
        }
        return replyRepository.findByTicketIdOrderByCreatedAtAsc(ticketId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ReplyResponse updateReply(Long replyId, UpdateReplyRequest req) {
        TicketReply r = replyRepository.findById(replyId)
                .orElseThrow(() -> new EntityNotFoundException("TicketReply", replyId));
        Ticket t = r.getTicket();

        if (t.getStatus() == TicketStatus.CLOSED) {
            throw new ResponseStatusException(CONFLICT, "Ticket is CLOSED");
        }

        Long uid = currentUserService.requireCurrentUserId();
        if (!uid.equals(r.getAuthorUserId())) {
            throw new ResponseStatusException(FORBIDDEN, "Only author can modify");
        }

        String msg = contentModerationService.censorIfProfane(req.message()).trim();
        r.setMessage(msg);
        r = replyRepository.save(r);

        t.setLastActivityAt(LocalDateTime.now());
        ticketRepository.save(t);

        return toResponse(r);
    }

    @Transactional
    public void deleteReply(Long replyId) {
        TicketReply r = replyRepository.findById(replyId)
                .orElseThrow(() -> new EntityNotFoundException("TicketReply", replyId));

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
                r.getCreatedAt()
        );
    }
}

