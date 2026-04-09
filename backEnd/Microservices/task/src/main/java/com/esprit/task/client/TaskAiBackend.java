package com.esprit.task.client;

/** Single AI backend (AIMODEL Spring + Ollama). */
public enum TaskAiBackend {
    OLLAMA;

    public static TaskAiBackend fromHeader(String raw) {
        if (raw == null || raw.isBlank()) {
            return OLLAMA;
        }
        return OLLAMA;
    }
}
