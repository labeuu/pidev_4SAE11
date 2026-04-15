package org.example.subcontracting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubcontractMatchCandidateDto {

    /** Identifiant du freelancer (nécessaire pour créer la sous-traitance). */
    private Long freelancerId;

    private String fullName;

    private String email;

    /** Score de compatibilité IA (0–100). */
    private Integer matchScore;

    private List<String> matchReasons;

    /** Score de confiance / performance sur la plateforme (0–100), calculé côté backend. */
    private Integer trustScore;

    /** Nombre de sous-traitances passées entre le principal et ce candidat. */
    private Long previousCollaborations;

    /** HIGHLY_RECOMMENDED | RECOMMENDED | POSSIBLE */
    private String recommendation;
}
