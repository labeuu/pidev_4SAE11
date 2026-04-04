package com.esprit.task.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDto {
    private Long id;
    private Long clientId;
    private String title;
    /** Present on Project service ProjectResponse; used for AI context. */
    private String description;
    private LocalDateTime deadline;
}
