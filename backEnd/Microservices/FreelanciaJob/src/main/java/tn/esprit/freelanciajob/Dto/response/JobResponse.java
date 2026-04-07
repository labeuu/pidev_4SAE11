package tn.esprit.freelanciajob.Dto.response;

import lombok.Data;
import tn.esprit.freelanciajob.Dto.Skills;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class JobResponse {
    private Long id;
    private Long clientId;
    private String clientType;
    private String companyName;
    private String title;
    private String description;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private String currency;
    private LocalDateTime deadline;
    private String category;
    private String locationType;
    private String status;
    private List<Long> requiredSkillIds;
    private List<Skills> skills;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
