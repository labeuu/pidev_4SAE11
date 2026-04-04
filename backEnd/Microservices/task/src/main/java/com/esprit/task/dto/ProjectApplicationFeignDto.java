package com.esprit.task.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectApplicationFeignDto {

    private Long id;
    private Long freelanceId;
    /** Serialized enum name e.g. ACCEPTED */
    private String status;
    private NestedProject project;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NestedProject {
        private Long id;
        private String title;
    }
}
