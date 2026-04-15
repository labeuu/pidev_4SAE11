package org.example.subcontracting.coach.config;

import org.example.subcontracting.coach.CoachWalletProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CoachWalletProperties.class)
public class CoachModuleConfiguration {
}
