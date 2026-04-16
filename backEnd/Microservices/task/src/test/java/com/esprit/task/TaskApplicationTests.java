package com.esprit.task;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=optional:configserver:http://127.0.0.1:65534",
        "spring.cloud.openfeign.client.config.Project.url=http://127.0.0.1:8084",
        "spring.cloud.openfeign.client.config.Contract.url=http://127.0.0.1:8083",
        "spring.cloud.openfeign.client.config.user.url=http://127.0.0.1:8090"
})
class TaskApplicationTests {

    @Test
    void contextLoads() {
    }
}
