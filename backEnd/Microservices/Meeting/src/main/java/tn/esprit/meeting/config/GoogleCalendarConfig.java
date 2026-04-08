package tn.esprit.meeting.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Collections;

/**
 * Configures Google Calendar API client.
 * Only active when google.calendar.enabled=true AND credentials file is present.
 * Mirrors the Planning service pattern so the same credential file can be reused.
 *
 * Setup:
 *   1. Create a Google Cloud project, enable Calendar API.
 *   2. Create a Service Account, download the JSON key.
 *   3. Share a Google Calendar with the service account email (give it "Make changes to events" permission).
 *   4. Set GOOGLE_APPLICATION_CREDENTIALS=/path/to/key.json  (or google.calendar.credentials-path).
 *   5. Set google.calendar.enabled=true and google.calendar.default-calendar-id=<calendar-email>.
 */
@Configuration
@Slf4j
public class GoogleCalendarConfig {

    @Bean
    @ConditionalOnProperty(prefix = "google.calendar", name = "enabled", havingValue = "true")
    public Calendar googleCalendar(
            @Value("${google.calendar.credentials-path:}") String credentialsPath)
            throws IOException, GeneralSecurityException {

        String pathToUse = (credentialsPath != null && !credentialsPath.isBlank())
                ? credentialsPath.trim()
                : System.getenv("GOOGLE_APPLICATION_CREDENTIALS");

        if (pathToUse == null || pathToUse.isBlank()) {
            log.warn("[Meeting] Google Calendar enabled but credentials not configured. Set GOOGLE_APPLICATION_CREDENTIALS or google.calendar.credentials-path.");
            return null;
        }
        Path path = Path.of(pathToUse);
        if (!Files.isRegularFile(path)) {
            log.warn("[Meeting] Google Calendar credentials file not found: {}. Calendar operations will be skipped.", pathToUse);
            return null;
        }
        try (FileInputStream in = new FileInputStream(path.toFile())) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(in)
                    .createScoped(Collections.singleton(CalendarScopes.CALENDAR));
            log.info("[Meeting] Google Calendar API initialized successfully.");
            return new Calendar.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName("Meeting Service")
                    .build();
        }
    }
}
