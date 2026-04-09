package com.esprit.aimodel.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiErrorEnvelope(@JsonProperty("success") boolean success, @JsonProperty("error") ErrorBody error) {

    public record ErrorBody(@JsonProperty("message") String message) {}

    public static AiErrorEnvelope of(String message) {
        return new AiErrorEnvelope(false, new ErrorBody(message));
    }
}
