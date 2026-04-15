package com.esprit.aimodel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiGenerateResponse {
    private boolean success;
    private String data;
}
