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
public class MissionFinanceRowDto {
    private Long subcontractId;
    private String title;
    private BigDecimal budget;
    private String status;
    private Boolean hadLateDeliverables;
}
