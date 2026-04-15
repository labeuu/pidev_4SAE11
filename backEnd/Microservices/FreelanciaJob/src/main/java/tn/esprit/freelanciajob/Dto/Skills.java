package tn.esprit.freelanciajob.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Skills {
    private Long id;
    private String name;
    private String domain;
    private String description;
    private Long userId;
}
