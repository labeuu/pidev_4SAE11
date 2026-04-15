package com.esprit.task;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=optional:configserver:http://127.0.0.1:65534",
        "project.service.url=http://localhost:8084",
        "contract.service.url=http://localhost:8083",
        "user.service.url=http://localhost:8090"
})
class TaskApplicationTests {

    @Test
    void contextLoads() {
    }
}
