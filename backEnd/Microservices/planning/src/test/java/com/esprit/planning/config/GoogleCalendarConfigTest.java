package com.esprit.planning.config;

import com.google.api.services.calendar.Calendar;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Loads {@link GoogleCalendarConfig} with a missing credentials file so the bean factory
 * returns null without requiring real Google credentials (covers warning / no-op paths).
 */
@SpringBootTest(classes = GoogleCalendarConfig.class)
class GoogleCalendarConfigTest {

    @DynamicPropertySource
    static void googleCalendarProperties(DynamicPropertyRegistry registry) {
        registry.add("google.calendar.enabled", () -> "true");
        registry.add("google.calendar.credentials-path", () -> "__planning_test_missing_credentials__.json");
    }

    @Autowired(required = false)
    private Calendar googleCalendar;

    @Test
    void googleCalendarBean_absentWhenCredentialsFileMissing() {
        assertThat(googleCalendar).isNull();
    }
}
