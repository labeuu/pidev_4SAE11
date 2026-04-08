package tn.esprit.meeting.service;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tn.esprit.meeting.dto.MeetingResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Creates and manages Google Calendar events with Google Meet conference links.
 *
 * How Meet links are generated:
 *   - Insert a Calendar event with conferenceData.createRequest (requestId must be unique per event).
 *   - Pass conferenceDataVersion=1 in the insert/patch call.
 *   - Google responds with conferenceData.entryPoints[type=video].uri → the Meet URL.
 *
 * Prerequisites (service account setup):
 *   1. Google Cloud project with Calendar API enabled.
 *   2. Service Account JSON key (same file as Planning service).
 *   3. The service account must be granted "Make changes to events" on a shared calendar,
 *      OR use Google Workspace domain-wide delegation to act as a user.
 *   4. Set google.calendar.enabled=true and supply the credentials path.
 *
 * When Google Calendar is disabled/unavailable, all methods fail gracefully with empty optionals.
 */
@Service
@Slf4j
public class GoogleMeetService {

    private final Calendar calendar;
    private final boolean enabled;
    private final String defaultCalendarId;
    private final String timezone;

    public GoogleMeetService(
            @Autowired(required = false) Calendar calendar,
            @Value("${google.calendar.enabled:false}") boolean enabled,
            @Value("${google.calendar.default-calendar-id:primary}") String defaultCalendarId,
            @Value("${google.calendar.timezone:Africa/Tunis}") String timezone) {
        this.calendar = calendar;
        this.enabled = enabled;
        this.defaultCalendarId = defaultCalendarId;
        this.timezone = timezone;
    }

    public boolean isAvailable() {
        return enabled && calendar != null;
    }

    /**
     * Creates a Google Calendar event with an auto-generated Google Meet link.
     *
     * @param calendarId     Calendar to create event in (null → default)
     * @param title          Event title / summary
     * @param agenda         Event description / agenda
     * @param startTime      Meeting start
     * @param endTime        Meeting end
     * @param attendeeEmails List of attendee emails (optional, may be empty)
     * @return EventResult with eventId and meetLink, or empty on failure / unavailable
     */
    public Optional<EventResult> createMeetingEvent(
            String calendarId,
            String title,
            String agenda,
            LocalDateTime startTime,
            LocalDateTime endTime,
            List<String> attendeeEmails) {

        if (!isAvailable()) {
            log.info("[GoogleMeetService] Calendar not available — returning empty (no Meet link).");
            return Optional.empty();
        }

        String calId = resolve(calendarId);
        try {
            Event event = new Event();
            event.setSummary(title);
            if (agenda != null && !agenda.isBlank()) {
                event.setDescription(agenda);
            }

            // Times
            ZoneId zone = resolveZone();
            event.setStart(new EventDateTime()
                    .setDateTime(toGoogleDateTime(startTime, zone))
                    .setTimeZone(zone.getId()));
            event.setEnd(new EventDateTime()
                    .setDateTime(toGoogleDateTime(endTime, zone))
                    .setTimeZone(zone.getId()));

            // Attendees
            if (attendeeEmails != null && !attendeeEmails.isEmpty()) {
                List<EventAttendee> attendees = attendeeEmails.stream()
                        .filter(e -> e != null && !e.isBlank())
                        .map(e -> new EventAttendee().setEmail(e))
                        .toList();
                if (!attendees.isEmpty()) {
                    event.setAttendees(attendees);
                }
            }

            // Conference data request → tells Google to generate a Meet link
            ConferenceData conferenceData = new ConferenceData();
            conferenceData.setCreateRequest(
                    new CreateConferenceRequest()
                            .setRequestId(UUID.randomUUID().toString())
                            .setConferenceSolutionKey(
                                    new ConferenceSolutionKey().setType("hangoutsMeet")));
            event.setConferenceData(conferenceData);

            // conferenceDataVersion=1 is required to trigger Meet link generation
            Event created = calendar.events()
                    .insert(calId, event)
                    .setConferenceDataVersion(1)
                    .execute();

            String eventId = created.getId();
            String meetLink = extractMeetLink(created);

            log.info("[GoogleMeetService] Created event id={} meetLink={}", eventId, meetLink);
            return Optional.of(new EventResult(eventId, meetLink, calId));

        } catch (IOException e) {
            log.warn("[GoogleMeetService] Failed to create calendar event: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Updates an existing calendar event (title, agenda, times).
     */
    public boolean updateEvent(String calendarId, String eventId, String title, String agenda,
                               LocalDateTime startTime, LocalDateTime endTime) {
        if (!isAvailable() || eventId == null) return false;
        String calId = resolve(calendarId);
        try {
            Event event = calendar.events().get(calId, eventId).execute();
            if (title != null) event.setSummary(title);
            if (agenda != null) event.setDescription(agenda);
            ZoneId zone = resolveZone();
            if (startTime != null) {
                event.setStart(new EventDateTime().setDateTime(toGoogleDateTime(startTime, zone)).setTimeZone(zone.getId()));
            }
            if (endTime != null) {
                event.setEnd(new EventDateTime().setDateTime(toGoogleDateTime(endTime, zone)).setTimeZone(zone.getId()));
            }
            calendar.events().update(calId, eventId, event).execute();
            return true;
        } catch (IOException e) {
            log.warn("[GoogleMeetService] Failed to update event {}: {}", eventId, e.getMessage());
            return false;
        }
    }

    /**
     * Cancels a calendar event by setting its status to "cancelled".
     */
    public boolean cancelEvent(String calendarId, String eventId) {
        if (!isAvailable() || eventId == null || eventId.isBlank()) return false;
        String calId = resolve(calendarId);
        try {
            calendar.events().delete(calId, eventId).execute();
            log.info("[GoogleMeetService] Deleted calendar event id={}", eventId);
            return true;
        } catch (IOException e) {
            log.warn("[GoogleMeetService] Failed to cancel event {}: {}", eventId, e.getMessage());
            return false;
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String resolve(String calendarId) {
        return (calendarId != null && !calendarId.isBlank()) ? calendarId : defaultCalendarId;
    }

    private ZoneId resolveZone() {
        try {
            return ZoneId.of(timezone);
        } catch (Exception e) {
            return ZoneId.systemDefault();
        }
    }

    private DateTime toGoogleDateTime(LocalDateTime ldt, ZoneId zone) {
        return new DateTime(ldt.atZone(zone).toInstant().toString());
    }

    private String extractMeetLink(Event event) {
        if (event.getConferenceData() == null) return null;
        List<EntryPoint> entryPoints = event.getConferenceData().getEntryPoints();
        if (entryPoints == null) return null;
        return entryPoints.stream()
                .filter(ep -> "video".equals(ep.getEntryPointType()))
                .map(EntryPoint::getUri)
                .findFirst()
                .orElse(null);
    }

    // ── Result record ─────────────────────────────────────────────────────────

    public record EventResult(String eventId, String meetLink, String calendarId) {}
}
