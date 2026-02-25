package org.example.offer.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Requête pour traduire des textes via Google Translate API.
 * Langues supportées : FR, EN, AR.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TranslateTextsRequest {

    @NotEmpty(message = "At least one text is required")
    @Size(max = 10, message = "Maximum 10 texts per request")
    private List<String> texts;

    /** Code langue cible : fr, en, ar */
    @NotNull(message = "Target language is required")
    private String targetLanguage;
}
