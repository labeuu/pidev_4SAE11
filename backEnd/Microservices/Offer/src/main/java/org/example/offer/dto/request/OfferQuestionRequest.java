package org.example.offer.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OfferQuestionRequest {

    @NotBlank(message = "Question text is required")
    @Size(min = 10, max = 1000)
    private String questionText;
}
