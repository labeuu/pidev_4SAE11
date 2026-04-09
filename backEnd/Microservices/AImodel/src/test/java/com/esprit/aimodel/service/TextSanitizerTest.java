package com.esprit.aimodel.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TextSanitizerTest {

    @Test
    void stripModelNoiseRemovesMarkdownFence() {
        String raw = "```json\n{\"tasks\":[]}\n```";
        assertThat(TextSanitizer.stripModelNoise(raw)).isEqualTo("{\"tasks\":[]}");
    }

    @Test
    void taskPlannerPromptPrefixContainsTasksKeyInstruction() {
        assertThat(TextSanitizer.taskPlannerPromptPrefix()).contains("\"tasks\"");
    }

    @Test
    void subtaskPlannerPromptPrefixContainsSubtasksKeyInstruction() {
        assertThat(TextSanitizer.subtaskPlannerPromptPrefix()).contains("\"subtasks\"");
    }
}
