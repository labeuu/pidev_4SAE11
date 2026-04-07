package tn.esprit.project.Dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JointProjectItem {
    private Long id;
    private String title;
    private String status;
    private String category;
}
