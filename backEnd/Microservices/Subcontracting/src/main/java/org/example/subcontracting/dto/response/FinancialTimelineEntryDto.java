package org.example.subcontracting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialTimelineEntryDto {
    /** ISO-8601 date (mission created or deadline) */
    private String date;
    private String label;
    private BigDecimal amount;
}
