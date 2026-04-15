package org.example.subcontracting.config;

import java.util.Arrays;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.example.subcontracting.entity.SubcontractCategory;
import org.example.subcontracting.entity.SubcontractMediaType;
import org.example.subcontracting.entity.SubcontractStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubcontractDataSanitizer {

    private static final Logger log = LoggerFactory.getLogger(SubcontractDataSanitizer.class);

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void sanitizeEnumColumns() {
        try {
            int repairedCategory = jdbcTemplate.update(
                    "UPDATE subcontracts " +
                    "SET category = 'DEVELOPMENT' " +
                    "WHERE category IS NULL OR TRIM(category) = '' OR UPPER(TRIM(category)) NOT IN (" + inValues(SubcontractCategory.values()) + ")"
            );

            int repairedStatus = jdbcTemplate.update(
                    "UPDATE subcontracts " +
                    "SET status = 'DRAFT' " +
                    "WHERE status IS NULL OR TRIM(status) = '' OR UPPER(TRIM(status)) NOT IN (" + inValues(SubcontractStatus.values()) + ")"
            );

            int repairedMediaType = jdbcTemplate.update(
                    "UPDATE subcontracts " +
                    "SET media_type = NULL " +
                    "WHERE media_type IS NOT NULL AND (TRIM(media_type) = '' OR UPPER(TRIM(media_type)) NOT IN (" + inValues(SubcontractMediaType.values()) + "))"
            );

            int repairedRoundCount = jdbcTemplate.update(
                    "UPDATE subcontracts SET negotiation_round_count = 0 WHERE negotiation_round_count IS NULL"
            );

            int repairedNegotiationStatus = jdbcTemplate.update(
                    "UPDATE subcontracts SET negotiation_status = 'NONE' WHERE negotiation_status IS NULL OR TRIM(negotiation_status) = ''"
            );

            if (repairedCategory + repairedStatus + repairedMediaType + repairedRoundCount + repairedNegotiationStatus > 0) {
                log.warn(
                        "Sanitized subcontract data: category={}, status={}, media_type={}, negotiation_round_count={}, negotiation_status={}",
                        repairedCategory, repairedStatus, repairedMediaType, repairedRoundCount, repairedNegotiationStatus
                );
            }
        } catch (DataAccessException ex) {
            // Keep service startup resilient in fresh environments.
            log.debug("Skipped subcontract data sanitization: {}", ex.getMessage());
        }
    }

    private static String inValues(Enum<?>[] enums) {
        return Arrays.stream(enums)
                .map(Enum::name)
                .map(v -> "'" + v + "'")
                .collect(Collectors.joining(","));
    }
}
