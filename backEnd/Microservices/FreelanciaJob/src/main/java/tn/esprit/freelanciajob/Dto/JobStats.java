package tn.esprit.freelanciajob.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobStats {
    private Long jobId;
    private String jobTitle;
    private Long applicationsCount;
}
