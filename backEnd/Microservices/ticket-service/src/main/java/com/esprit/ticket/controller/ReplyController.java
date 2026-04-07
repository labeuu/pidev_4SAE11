package com.esprit.ticket.controller;

import com.esprit.ticket.dto.reply.CreateReplyRequest;
import com.esprit.ticket.dto.reply.ReplyResponse;
import com.esprit.ticket.dto.reply.UpdateReplyRequest;
import com.esprit.ticket.service.ReplyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/replies")
@RequiredArgsConstructor
public class ReplyController {

    private final ReplyService replyService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReplyResponse create(@Valid @RequestBody CreateReplyRequest req) {
        return replyService.addReply(req);
    }

    @GetMapping("/{ticketId}")
    public List<ReplyResponse> getByTicket(@PathVariable Long ticketId) {
        return replyService.getByTicketId(ticketId);
    }

    @PutMapping("/{id}")
    public ReplyResponse update(@PathVariable Long id, @Valid @RequestBody UpdateReplyRequest req) {
        return replyService.updateReply(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        replyService.deleteReply(id);
    }
}

