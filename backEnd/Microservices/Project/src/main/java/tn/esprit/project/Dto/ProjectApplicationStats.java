package tn.esprit.project.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectApplicationStats {

    private Long projectId;
    private String projectTitle;
    private Long applicationsCount;

}