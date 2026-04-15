package org.example.subcontracting.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubcontractMessageRequest {

    @NotBlank(message = "Le message est requis")
    @Size(max = 2000, message = "Le message ne doit pas dépasser 2000 caractères")
    private String message;
}
