package com.esprit.aimodel;

import com.esprit.aimodel.config.AImodelProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
@EnableConfigurationProperties(AImodelProperties.class)
public class AImodelApplication {

    public static void main(String[] args) {
        SpringApplication.run(AImodelApplication.class, args);
    }
}
