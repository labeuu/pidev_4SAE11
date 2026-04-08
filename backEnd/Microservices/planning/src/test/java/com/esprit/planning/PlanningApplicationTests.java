package com.esprit.planning;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=optional:configserver:http://127.0.0.1:65534"
})
class PlanningApplicationTests {

    @Test
    void contextLoads() {
    }

}
