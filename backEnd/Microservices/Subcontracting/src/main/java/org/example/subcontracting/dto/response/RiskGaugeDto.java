package org.example.subcontracting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskGaugeDto {
    private String key;
    private String label;
    private Integer score;
    private String level;
    private String explanation;
}
