package org.example.subcontracting.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PortfolioSkillDto {
    private Long id;
    private String name;
    private Long userId;
    private List<String> domains;
}
