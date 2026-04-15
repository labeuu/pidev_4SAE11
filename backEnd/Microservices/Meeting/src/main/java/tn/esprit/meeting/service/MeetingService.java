package tn.esprit.meeting.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.meeting.client.UserClient;
import tn.esprit.meeting.dto.*;
import tn.esprit.meeting.entity.Meeting;
import tn.esprit.meeting.enums.MeetingStatus;
import tn.esprit.meeting.enums.MeetingType;
import tn.esprit.meeting.exception.MeetingNotFoundException;
import tn.esprit.meeting.exception.MeetingValidationException;
import tn.esprit.meeting.repository.MeetingRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final GoogleMeetService googleMeetService;
    private final UserClient userClient;

    // ── Create ────────────────────────────────────────────────────────────────

    public MeetingResponse createMeeting(Long clientId, CreateMeetingRequest req) {
        validate(req);

        Meeting meeting = Meeting.builder()
                .clientId(clientId)
                .freelancerId(req.getFreelancerId())
                .title(req.getTitle())
                .agenda(req.getAgenda())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .meetingType(req.getMeetingType() != null ? req.getMeetingType() : MeetingType.VIDEO_CALL)
                .status(MeetingStatus.PENDING)
                .projectId(req.getProjectId())
                .contractId(req.getContractId())
                .build();

        Meeting saved = meetingRepository.save(meeting);
        log.info("[MeetingService] Meeting {} created by client {} with freelancer {}", saved.getId(), clientId, req.getFreelancerId());
        return toResponse(saved);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    public MeetingResponse updateMeeting(Long meetingId, Long requesterId, UpdateMeetingRequest req) {
        Meeting meeting = findOrThrow(meetingId);
        assertIsClient(meeting, requesterId);

        if (meeting.getStatus() != MeetingStatus.PENDING) {
            throw new MeetingValidationException("Only PENDING meetings can be edited");
        }

        if (req.getTitle() != null) meeting.setTitle(req.getTitle());
        if (req.getAgenda() != null) meeting.setAgenda(req.getAgenda());
        if (req.getStartTime() != null) meeting.setStartTime(req.getStartTime());
        if (req.getEndTime() != null) meeting.setEndTime(req.getEndTime());
        if (req.getMeetingType() != null) meeting.setMeetingType(req.getMeetingType());

        return toResponse(meetingRepository.save(meeting));
    }

    // ── Status transitions ────────────────────────────────────────────────────

    public MeetingResponse updateStatus(Long meetingId, Long requesterId, StatusUpdateRequest req) {
        Meeting meeting = findOrThrow(meetingId);
        MeetingStatus newStatus = req.getStatus();

        switch (newStatus) {
            case ACCEPTED -> {
                // Only the FREELANCER can accept
                if (!meeting.getFreelancerId().equals(requesterId)) {
                    throw new MeetingValidationException("Only the invited freelancer can accept this meeting");
                }
                if (meeting.getStatus() != MeetingStatus.PENDING) {
                    throw new MeetingValidationException("Meeting is not in PENDING state");
                }
                meeting.setStatus(MeetingStatus.ACCEPTED);
                // Create Google Calendar event with Meet link
                createCalendarEvent(meeting);
            }
            case DECLINED -> {
                if (!meeting.getFreelancerId().equals(requesterId)) {
                    throw new MeetingValidationException("Only the invited freelancer can decline");
                }
                if (meeting.getStatus() != MeetingStatus.PENDING) {
                    throw new MeetingValidationException("Meeting is not in PENDING state");
                }
                meeting.setStatus(MeetingStatus.DECLINED);
                meeting.setCancellationReason(req.getReason());
            }
            case CANCELLED -> {
                // Either party can cancel
                boolean isParticipant = meeting.getClientId().equals(requesterId)
                        || meeting.getFreelancerId().equals(requesterId);
                if (!isParticipant) {
                    throw new MeetingValidationException("You are not a participant of this meeting");
                }
                if (meeting.getStatus() == MeetingStatus.COMPLETED) {
                    throw new MeetingValidationException("Cannot cancel a completed meeting");
                }
                meeting.setStatus(MeetingStatus.CANCELLED);
                meeting.setCancellationReason(req.getReason());
                // Remove calendar event
                if (meeting.getGoogleEventId() != null) {
                    googleMeetService.cancelEvent(meeting.getCalendarId(), meeting.getGoogleEventId());
                }
            }
            case COMPLETED -> {
                // System-driven (or admin mark-as-complete)
                if (meeting.getStatus() != MeetingStatus.ACCEPTED) {
                    throw new MeetingValidationException("Only ACCEPTED meetings can be completed");
                }
                meeting.setStatus(MeetingStatus.COMPLETED);
            }
            default -> throw new MeetingValidationException("Invalid status transition to: " + newStatus);
        }

        return toResponse(meetingRepository.save(meeting));
    }

    // ── Reads ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public MeetingResponse getById(Long meetingId) {
        return toResponse(findOrThrow(meetingId));
    }

    @Transactional(readOnly = true)
    public List<MeetingResponse> getMeetingsForUser(Long userId) {
        return meetingRepository.findAllByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MeetingResponse> getUpcomingMeetings(Long userId) {
        return meetingRepository.findUpcomingByUserId(userId, LocalDateTime.now()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MeetingResponse> getMeetingsByStatus(Long userId, MeetingStatus status) {
        return meetingRepository.findByUserIdAndStatus(userId, status).stream()
                .map(this::toResponse)
                .toList();
    }

    // ── Delete (CLIENT only, PENDING only) ────────────────────────────────────

    public void deleteMeeting(Long meetingId, Long requesterId) {
        Meeting meeting = findOrThrow(meetingId);
        assertIsClient(meeting, requesterId);
        if (meeting.getStatus() != MeetingStatus.PENDING) {
            throw new MeetingValidationException("Only PENDING meetings can be deleted");
        }
        meetingRepository.delete(meeting);
    }

    // ── Google Calendar / Meet creation ───────────────────────────────────────

    private void createCalendarEvent(Meeting meeting) {
        if (meeting.getMeetingType() == MeetingType.IN_PERSON) return;
        if (googleMeetService.isAvailable()) {
            try {
                List<String> emails = resolveEmails(meeting.getClientId(), meeting.getFreelancerId());
                Optional<GoogleMeetService.EventResult> result = googleMeetService.createMeetingEvent(
                        null,
                        meeting.getTitle(),
                        meeting.getAgenda(),
                        meeting.getStartTime(),
                        meeting.getEndTime(),
                        emails);
                result.ifPresent(r -> {
                    meeting.setGoogleEventId(r.eventId());
                    meeting.setCalendarId(r.calendarId());
                    // Only use the Google Meet link if one was actually returned
                    if (r.meetLink() != null && !r.meetLink().isBlank()) {
                        meeting.setMeetLink(r.meetLink());
                    }
                    log.info("[MeetingService] Calendar event created for meeting {}, meetLink={}", meeting.getId(), r.meetLink());
                });
            } catch (Exception e) {
                log.warn("[MeetingService] Error creating calendar event for meeting {}: {}", meeting.getId(), e.getMessage());
            }
        }
        // Fallback: if no Meet link was obtained (Google unavailable or service account
        // cannot generate Meet links), create a Jitsi Meet room — free, no account required.
        if (meeting.getMeetLink() == null || meeting.getMeetLink().isBlank()) {
            String jitsiLink = "https://meet.jit.si/SmartFreelanceMeeting-" + meeting.getId();
            meeting.setMeetLink(jitsiLink);
            log.info("[MeetingService] Using Jitsi Meet fallback for meeting {}: {}", meeting.getId(), jitsiLink);
        }
    }

    private List<String> resolveEmails(Long clientId, Long freelancerId) {
        try {
            UserDto client = userClient.getUserById(clientId);
            UserDto freelancer = userClient.getUserById(freelancerId);
            return List.of(client.getEmail(), freelancer.getEmail())
                    .stream()
                    .filter(e -> e != null && !e.isBlank())
                    .toList();
        } catch (Exception e) {
            log.warn("[MeetingService] Could not resolve attendee emails: {}", e.getMessage());
            return List.of();
        }
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private void validate(CreateMeetingRequest req) {
        if (req.getEndTime() != null && req.getStartTime() != null
                && !req.getEndTime().isAfter(req.getStartTime())) {
            throw new MeetingValidationException("End time must be after start time");
        }
        if (req.getEndTime() != null && req.getStartTime() != null) {
            long minutes = java.time.Duration.between(req.getStartTime(), req.getEndTime()).toMinutes();
            if (minutes < 15) {
                throw new MeetingValidationException("Meeting must be at least 15 minutes long");
            }
            if (minutes > 480) {
                throw new MeetingValidationException("Meeting cannot exceed 8 hours");
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Meeting findOrThrow(Long id) {
        return meetingRepository.findById(id)
                .orElseThrow(() -> new MeetingNotFoundException("Meeting not found with id: " + id));
    }

    private void assertIsClient(Meeting meeting, Long requesterId) {
        if (!meeting.getClientId().equals(requesterId)) {
            throw new MeetingValidationException("Only the meeting organiser (CLIENT) can perform this action");
        }
    }

    private MeetingResponse toResponse(Meeting m) {
        String clientName = resolveName(m.getClientId());
        String freelancerName = resolveName(m.getFreelancerId());

        LocalDateTime now = LocalDateTime.now();
        boolean canJoinNow = m.getStatus() == MeetingStatus.ACCEPTED
                && m.getMeetLink() != null
                && !m.getMeetLink().isBlank()
                && now.isAfter(m.getStartTime().minusMinutes(15))
                && now.isBefore(m.getEndTime().plusMinutes(5));

        return MeetingResponse.builder()
                .id(m.getId())
                .clientId(m.getClientId())
                .freelancerId(m.getFreelancerId())
                .clientName(clientName)
                .freelancerName(freelancerName)
                .title(m.getTitle())
                .agenda(m.getAgenda())
                .startTime(m.getStartTime())
                .endTime(m.getEndTime())
                .meetingType(m.getMeetingType())
                .status(m.getStatus())
                .meetLink(m.getMeetLink())
                .googleEventId(m.getGoogleEventId())
                .projectId(m.getProjectId())
                .contractId(m.getContractId())
                .cancellationReason(m.getCancellationReason())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .canJoinNow(canJoinNow)
                .build();
    }

    private String resolveName(Long userId) {
        try {
            UserDto user = userClient.getUserById(userId);
            return user.getFullName();
        } catch (Exception e) {
            return "User #" + userId;
        }
    }
}
