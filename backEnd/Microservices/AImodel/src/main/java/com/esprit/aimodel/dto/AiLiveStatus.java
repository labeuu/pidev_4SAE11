package com.esprit.aimodel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiLiveStatus {
    private String service;
    private String status;
    private boolean ollamaReachable;
    private String model;
    private boolean modelReady;
}
