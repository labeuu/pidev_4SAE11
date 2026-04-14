package tn.esprit.freelanciajob.Dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TranslationRequest {

    @NotBlank
    private String text;

    @NotBlank
    private String targetLang;

    private String sourceLang = "auto";
}
