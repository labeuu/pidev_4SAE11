package tn.esprit.project.Dto;

import lombok.Data;

@Data
public class Skills {
    private Long id;
    private String name;
    private String domain;
    private String description;
    private Long userId;
}
