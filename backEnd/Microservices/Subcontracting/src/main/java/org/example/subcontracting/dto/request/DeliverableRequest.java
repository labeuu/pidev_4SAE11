package org.example.subcontracting.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DeliverableRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private LocalDate deadline;
}
