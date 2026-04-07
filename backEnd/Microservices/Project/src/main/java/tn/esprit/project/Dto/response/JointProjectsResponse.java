package tn.esprit.project.Dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Projets où un client et un freelancer sont liés (projet du client + candidature du freelancer).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JointProjectsResponse {
    private long sharedProjectCount;
    private List<JointProjectItem> projects;
}
