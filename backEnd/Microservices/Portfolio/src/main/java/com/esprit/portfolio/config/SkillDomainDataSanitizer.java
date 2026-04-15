package com.esprit.portfolio.config;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.esprit.portfolio.entity.Domain;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SkillDomainDataSanitizer {

    private static final Logger log = LoggerFactory.getLogger(SkillDomainDataSanitizer.class);

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void sanitizeInvalidSkillDomains() {
        String allowedValues = Arrays.stream(Domain.values())
                .map(Enum::name)
                .map(name -> "'" + name + "'")
                .collect(Collectors.joining(","));

        String sql = "DELETE FROM skill_domains " +
                "WHERE domain IS NULL OR TRIM(domain) = '' OR domain NOT IN (" + allowedValues + ")";

        try {
            int deletedRows = jdbcTemplate.update(sql);
            if (deletedRows > 0) {
                log.warn("Removed {} invalid rows from skill_domains (unknown or empty domain values).", deletedRows);
            }
        } catch (DataAccessException ex) {
            // Keep startup resilient if the table does not exist yet.
            log.debug("Skipped skill_domains sanitization: {}", ex.getMessage());
        }
    }
}
