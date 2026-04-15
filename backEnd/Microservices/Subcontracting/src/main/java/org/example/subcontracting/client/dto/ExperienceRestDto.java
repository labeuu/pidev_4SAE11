package org.example.subcontracting.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExperienceRestDto {
    private Long id;
    private Long userId;
    private String title;
    private String description;
    private String companyOrClientName;
}
