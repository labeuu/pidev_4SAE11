package tn.esprit.freelanciajob.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExperienceDto {
    private Long id;
    private String title;
    private String type;
    private String domain;
    private String description;
    private String startDate;
    private String endDate;
    private String companyOrClientName;
    private List<String> keyTasks;
}
