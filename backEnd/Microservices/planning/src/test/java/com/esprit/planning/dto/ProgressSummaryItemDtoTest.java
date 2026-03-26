package com.esprit.planning.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ProgressSummaryItemDtoTest {

    @Test
    void builder_roundTrip() {
        LocalDateTime at = LocalDateTime.of(2026, 3, 1, 12, 0);
        ProgressSummaryItemDto dto = ProgressSummaryItemDto.builder()
                .projectId(1L)
                .contractId(9L)
                .currentProgressPercentage(75)
                .lastUpdateAt(at)
                .build();

        assertThat(dto.getProjectId()).isEqualTo(1L);
        assertThat(dto.getContractId()).isEqualTo(9L);
        assertThat(dto.getCurrentProgressPercentage()).isEqualTo(75);
        assertThat(dto.getLastUpdateAt()).isEqualTo(at);
    }

    @Test
    void noArgsAndAllArgsConstructors() {
        ProgressSummaryItemDto empty = new ProgressSummaryItemDto();
        assertThat(empty.getProjectId()).isNull();

        LocalDateTime at = LocalDateTime.now();
        ProgressSummaryItemDto full = new ProgressSummaryItemDto(2L, 3L, 50, at);
        assertThat(full.getProjectId()).isEqualTo(2L);
        assertThat(full.getContractId()).isEqualTo(3L);
    }

    @Test
    void settersAndGetters_mutateState() {
        ProgressSummaryItemDto dto = new ProgressSummaryItemDto();
        LocalDateTime at = LocalDateTime.of(2026, 6, 15, 9, 30);
        dto.setProjectId(100L);
        dto.setContractId(200L);
        dto.setCurrentProgressPercentage(33);
        dto.setLastUpdateAt(at);

        assertThat(dto.getProjectId()).isEqualTo(100L);
        assertThat(dto.getContractId()).isEqualTo(200L);
        assertThat(dto.getCurrentProgressPercentage()).isEqualTo(33);
        assertThat(dto.getLastUpdateAt()).isEqualTo(at);
    }
}
