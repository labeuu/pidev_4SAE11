package com.esprit.planning.client;

import com.esprit.planning.dto.CalendarEventDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Feign client for the Task microservice to fetch task deadline events for the Planning calendar.
 */
@FeignClient(name = "task", path = "/api/tasks")
public interface TaskClient {

    @GetMapping("/calendar-events")
    List<CalendarEventDto> getCalendarEvents(
            @RequestParam(value = "timeMin", required = false) String timeMin,
            @RequestParam(value = "timeMax", required = false) String timeMax,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "role", required = false) String role);
}
