package org.example.subcontracting.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.subcontracting.client.NotificationFeignClient;
import org.example.subcontracting.client.UserFeignClient;
import org.example.subcontracting.client.dto.NotificationRequestDto;
import org.example.subcontracting.client.dto.UserRemoteDto;
import org.example.subcontracting.dto.response.SubcontractMessageResponse;
import org.example.subcontracting.entity.Subcontract;
import org.example.subcontracting.entity.SubcontractMessage;
import org.example.subcontracting.repository.SubcontractMessageRepository;
import org.example.subcontracting.repository.SubcontractRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubcontractChatService {

    private final SubcontractRepository subcontractRepository;
    private final SubcontractMessageRepository messageRepository;
    private final UserFeignClient userFeignClient;
    private final NotificationFeignClient notificationFeignClient;
    private final SubcontractEmailService subcontractEmailService;

    @Transactional(readOnly = true)
    public List<SubcontractMessageResponse> getMessages(Long subcontractId, Long viewerUserId) {
        Subcontract subcontract = findSubcontractOrThrow(subcontractId);
        ensureParticipant(subcontract, viewerUserId);
        return messageRepository.findBySubcontractIdOrderByCreatedAtAsc(subcontractId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SubcontractMessageResponse sendMessage(Long subcontractId, Long senderUserId, String rawMessage) {
        Subcontract subcontract = findSubcontractOrThrow(subcontractId);
        ensureParticipant(subcontract, senderUserId);

        String message = rawMessage == null ? "" : rawMessage.trim();
        if (message.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le message est vide");
        }
        if (message.length() > 2000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le message dépasse 2000 caractères");
        }

        UserRemoteDto senderUser = fetchUserOrThrow(senderUserId);
        String senderName = fullNameOrFallback(senderUser, "Freelancer");

        SubcontractMessage saved = messageRepository.save(SubcontractMessage.builder()
                .subcontract(subcontract)
                .senderUserId(senderUserId)
                .senderName(senderName)
                .message(message)
                .build());

        Long recipientId = senderUserId.equals(subcontract.getMainFreelancerId())
                ? subcontract.getSubcontractorId()
                : subcontract.getMainFreelancerId();

        notifyRecipient(recipientId, subcontract, senderName, message);
        emailRecipient(recipientId, senderName, subcontract, message);

        return toResponse(saved);
    }

    private void notifyRecipient(Long recipientId, Subcontract subcontract, String senderName, String message) {
        try {
            notificationFeignClient.sendNotification(NotificationRequestDto.builder()
                    .userId(String.valueOf(recipientId))
                    .type("SUBCONTRACT_CHAT_MESSAGE")
                    .title("Nouveau message sur sous-traitance")
                    .body(senderName + " : " + shorten(message, 120))
                    .build());
        } catch (Exception e) {
            log.warn("[SUBCONTRACT-CHAT] Notification échouée recipient={}: {}", recipientId, e.getMessage());
        }
    }

    private void emailRecipient(Long recipientId, String senderName, Subcontract subcontract, String message) {
        try {
            UserRemoteDto recipient = fetchUserOrThrow(recipientId);
            if (recipient.getEmail() == null || recipient.getEmail().isBlank()) {
                return;
            }
            subcontractEmailService.sendChatMessageEmail(
                    recipient.getEmail().trim(),
                    fullNameOrFallback(recipient, "Freelancer"),
                    senderName,
                    subcontract.getTitle(),
                    message
            );
        } catch (Exception e) {
            log.warn("[SUBCONTRACT-CHAT] Email non envoyé recipient={}: {}", recipientId, e.getMessage());
        }
    }

    private Subcontract findSubcontractOrThrow(Long subcontractId) {
        return subcontractRepository.findById(subcontractId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sous-traitance introuvable"));
    }

    private void ensureParticipant(Subcontract subcontract, Long userId) {
        if (userId == null || userId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId invalide");
        }
        boolean allowed = userId.equals(subcontract.getMainFreelancerId()) || userId.equals(subcontract.getSubcontractorId());
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé à cette conversation");
        }
    }

    private UserRemoteDto fetchUserOrThrow(Long userId) {
        try {
            UserRemoteDto user = userFeignClient.getUserById(userId);
            if (user == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable");
            }
            return user;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service utilisateurs indisponible");
        }
    }

    private String fullNameOrFallback(UserRemoteDto user, String fallback) {
        String name = ((user.getFirstName() != null ? user.getFirstName() : "") + " "
                + (user.getLastName() != null ? user.getLastName() : "")).trim();
        return name.isBlank() ? fallback : name;
    }

    private SubcontractMessageResponse toResponse(SubcontractMessage message) {
        return SubcontractMessageResponse.builder()
                .id(message.getId())
                .subcontractId(message.getSubcontract().getId())
                .senderUserId(message.getSenderUserId())
                .senderName(message.getSenderName())
                .message(message.getMessage())
                .createdAt(message.getCreatedAt())
                .build();
    }

    private String shorten(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max) + "...";
    }
}
