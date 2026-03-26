package com.esprit.planning.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.calendar.Calendar;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Loads {@link GoogleCalendarConfig} with a syntactically valid service-account JSON on disk so the
 * {@link Calendar} bean is constructed (covers the success path in {@code googleCalendar}).
 */
@SpringBootTest(classes = GoogleCalendarConfig.class)
class GoogleCalendarSuccessfulBeanTest {

    private static final Path CREDENTIALS_FILE;

    static {
        try {
            CREDENTIALS_FILE = Files.createTempFile("planning-gcal-valid-", ".json");
            CREDENTIALS_FILE.toFile().deleteOnExit();
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            KeyPair kp = kpg.generateKeyPair();
            byte[] pkcs8 = kp.getPrivate().getEncoded();
            String pem = toPkcs8Pem(pkcs8);
            Map<String, String> json = new LinkedHashMap<>();
            json.put("type", "service_account");
            json.put("project_id", "planning-test");
            json.put("private_key_id", "test-key-id");
            json.put("private_key", pem);
            json.put("client_email", "planning-test@test-project.iam.gserviceaccount.com");
            json.put("client_id", "123456789");
            json.put("auth_uri", "https://accounts.google.com/o/oauth2/auth");
            json.put("token_uri", "https://oauth2.googleapis.com/token");
            new ObjectMapper().writeValue(CREDENTIALS_FILE.toFile(), json);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static String toPkcs8Pem(byte[] pkcs8Der) {
        String b64 = Base64.getEncoder().encodeToString(pkcs8Der);
        StringBuilder sb = new StringBuilder("-----BEGIN PRIVATE KEY-----\n");
        for (int i = 0; i < b64.length(); i += 64) {
            sb.append(b64, i, Math.min(i + 64, b64.length())).append('\n');
        }
        sb.append("-----END PRIVATE KEY-----\n");
        return sb.toString();
    }

    @DynamicPropertySource
    static void googleProps(DynamicPropertyRegistry registry) {
        registry.add("google.calendar.enabled", () -> "true");
        registry.add("google.calendar.credentials-path", () -> CREDENTIALS_FILE.toAbsolutePath().toString());
    }

    @Autowired(required = false)
    private Calendar googleCalendar;

    @Test
    void googleCalendarBean_createdWhenCredentialsFileValid() {
        assertThat(googleCalendar).isNotNull();
    }
}
