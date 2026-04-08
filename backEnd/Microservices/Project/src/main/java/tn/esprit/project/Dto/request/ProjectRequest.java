package tn.esprit.project.Dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectRequest {

    @NotNull
    private Long clientId;

    @NotBlank
    private String title;

    @NotBlank
    private String description;

    @NotNull
    @DecimalMin(value="0.0", inclusive=false)
    private BigDecimal budget;

    @NotNull
    @Future
    private LocalDateTime deadline;

    @NotBlank
    private String category;

    @NotEmpty
    private List<Long> skillIds;

    // 🆕 Permet d'envoyer des objets complexes (Nom + Domaines) à créer dans Portfolio
    private List<NewSkillRequest> newSkills;
}
