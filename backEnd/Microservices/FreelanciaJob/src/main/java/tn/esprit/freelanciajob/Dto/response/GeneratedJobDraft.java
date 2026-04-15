package tn.esprit.freelanciajob.Dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedJobDraft {
    private String title;
    private String description;
    private List<String> requiredSkills;   // skill names (frontend maps to IDs)
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private String currency;
    private Integer estimatedDurationWeeks;
    private String category;
    private String locationType;           // REMOTE | ONSITE | HYBRID
}
