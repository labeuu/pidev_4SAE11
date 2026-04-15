package com.esprit.planning.controller;

import com.esprit.planning.dto.CalendarEventDto;
import com.esprit.planning.service.CalendarEventService;
import com.esprit.planning.service.GoogleCalendarService;
import com.esprit.planning.service.ProgressUpdateService;
import com.google.api.services.calendar.model.Event;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API for calendar: sync project deadline to calendar for a freelancer, and list events in a time range.
 * When userId/role are provided, returns events scoped to that user; otherwise uses Google Calendar if available or DB events.
 * All endpoints are under /api/calendar.
 */
@RestController
@RequestMapping("/api/calendar")
@CrossOrigin(origins = "*")
@Tag(name = "Calendar", description = "Calendar events (Google Calendar when enabled, else from progress updates and project deadlines)")
public class CalendarController {

    private final GoogleCalendarService googleCalendarService;
    private final ProgressUpdateService progressUpdateService;
    private final CalendarEventService calendarEventService;

    public CalendarController(GoogleCalendarService googleCalendarService,
                              ProgressUpdateService progressUpdateService,
                              CalendarEventService calendarEventService) {
        this.googleCalendarService = googleCalendarService;
        this.progressUpdateService = progressUpdateService;
        this.calendarEventService = calendarEventService;
    }

    /** Ensures the project deadline is in the calendar for the given freelancer. Idempotent; notifies when first synced. */
    @PostMapping("/sync-project-deadline")
    @Operation(
            summary = "Sync project deadline to calendar",
            description = "Ensures the project deadline is added to the calendar for the given freelancer. Idempotent. Notifies the freelancer when the deadline is first added."
    )
    @ApiResponse(responseCode = "200", description = "Success")
    public ResponseEntity<Void> syncProjectDeadline(
            @Parameter(description = "Project ID", required = true) @RequestParam Long projectId,
            @Parameter(description = "Freelancer user ID (to notify)", required = true) @RequestParam Long freelancerId) {
        progressUpdateService.ensureProjectDeadlineInCalendarForUser(projectId, freelancerId);
        return ResponseEntity.ok().build();
    }

    /** Returns calendar events in the given time range. With userId/role, scopes to that user; else Google Calendar or DB. */
    @GetMapping("/events")
    @Operation(
            summary = "List calendar events",
            description = "Returns events in the given time range, optionally scoped to a user. When userId is provided, only events relevant to that client or freelancer are returned. When Google Calendar is enabled and no userId is passed, events come from Google; otherwise from progress updates (next due) and project deadlines. timeMin/timeMax accept ISO-8601 (e.g. with Z or offset)."
    )
    @ApiResponse(responseCode = "200", description = "Success")
    public ResponseEntity<List<CalendarEventDto>> listEvents(
            @Parameter(description = "Start of range (ISO-8601 date-time, e.g. 2026-02-28T03:19:42.848Z)")
            @RequestParam(required = false) String timeMin,
            @Parameter(description = "End of range (ISO-8601 date-time)")
            @RequestParam(required = false) String timeMax,
            @Parameter(description = "Calendar ID (default from config, used only when Google Calendar is enabled and no userId)")
            @RequestParam(required = false) String calendarId,
            @Parameter(description = "Current user ID – when set, only events for this client/freelancer are returned")
            @RequestParam(required = false) Long userId,
            @Parameter(description = "Current user role (CLIENT, FREELANCER, ADMIN) – used with userId for filtering")
            @RequestParam(required = false) String role,
            @RequestHeader(value = "X-User-Id", required = false) Long authenticatedUserId,
            @RequestHeader(value = "X-User-Role", required = false) String authenticatedRole) {
        Long effectiveUserId = authenticatedUserId != null ? authenticatedUserId : userId;
        String effectiveRole = authenticatedRole != null ? authenticatedRole : role;
        LocalDateTime min = parseDateTime(timeMin, LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0));
        LocalDateTime max = parseDateTime(timeMax, min.plusMonths(2));
        List<CalendarEventDto> dtos;
        boolean scopeToUser = effectiveUserId != null && !"ADMIN".equalsIgnoreCase(effectiveRole);
        if (scopeToUser) {
            dtos = calendarEventService.listEventsFromDb(min, max, effectiveUserId, effectiveRole);
        } else if (effectiveUserId == null && googleCalendarService.isAvailable()) {
            List<Event> events = googleCalendarService.listEvents(calendarId, min, max);
            dtos = events.stream().map(this::toDto).collect(Collectors.toList());
        } else {
            dtos = calendarEventService.listEventsFromDb(min, max);
        }
        return ResponseEntity.ok(dtos);
    }

    /** Parses ISO-8601 string (with Z or offset) to LocalDateTime in system default zone; on failure returns defaultValue. */
    private static LocalDateTime parseDateTime(String value, LocalDateTime defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        try {
            return LocalDateTime.ofInstant(Instant.parse(value), ZoneId.systemDefault());
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    /** Converts a Google Calendar Event to CalendarEventDto (id, summary, start, end, description). */
    private CalendarEventDto toDto(Event e) {
        LocalDateTime start = null;
        LocalDateTime end = null;
        if (e.getStart() != null && e.getStart().getDateTime() != null) {
            start = LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(e.getStart().getDateTime().getValue()),
                    ZoneId.systemDefault());
        }
        if (e.getEnd() != null && e.getEnd().getDateTime() != null) {
            end = LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(e.getEnd().getDateTime().getValue()),
                    ZoneId.systemDefault());
        }
        return CalendarEventDto.builder()
                .id(e.getId())
                .summary(e.getSummary())
                .start(start)
                .end(end)
                .description(e.getDescription())
                .build();
    }
}
