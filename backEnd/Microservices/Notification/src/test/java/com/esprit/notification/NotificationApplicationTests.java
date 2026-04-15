package com.esprit.notification;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.cloud.config.enabled=false",
    "notification.firebase.enabled=false"
})
@SpringBootTest
class NotificationApplicationTests {

    @Test
    void contextLoads() {
    }
}
