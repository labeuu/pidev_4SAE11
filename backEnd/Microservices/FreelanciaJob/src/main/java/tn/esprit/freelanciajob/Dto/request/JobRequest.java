package tn.esprit.freelanciajob.Dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import tn.esprit.freelanciajob.Entity.Enums.ClientType;
import tn.esprit.freelanciajob.Entity.Enums.LocationType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class JobRequest {

    @NotNull(message = "clientId is required")
    private Long clientId;

    @NotNull(message = "clientType is required")
    private ClientType clientType;

    /** Required only when clientType = COMPANY */
    private String companyName;

    @NotBlank(message = "title is required")
    @Size(min = 3, max = 255, message = "title must be between 3 and 255 characters")
    private String title;

    @NotBlank(message = "description is required")
    @Size(min = 10, message = "description must be at least 10 characters")
    private String description;

    @DecimalMin(value = "0.0", inclusive = false, message = "budgetMin must be positive")
    private BigDecimal budgetMin;

    @DecimalMin(value = "0.0", inclusive = false, message = "budgetMax must be positive")
    private BigDecimal budgetMax;

    private String currency;

    @Future(message = "deadline must be in the future")
    private LocalDateTime deadline;

    @NotBlank(message = "category is required")
    private String category;

    @NotNull(message = "locationType is required")
    private LocationType locationType;

    private List<Long> requiredSkillIds;
}
