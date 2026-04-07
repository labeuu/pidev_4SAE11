package org.example.subcontracting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "org.example.subcontracting.client")
@EnableScheduling
public class SubcontractingApplication {

    public static void main(String[] args) {
        SpringApplication.run(SubcontractingApplication.class, args);
    }
}
