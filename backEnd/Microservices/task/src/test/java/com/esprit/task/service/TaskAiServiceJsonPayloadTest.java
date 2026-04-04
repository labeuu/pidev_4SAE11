package com.esprit.task.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TaskAiServiceJsonPayloadTest {

    @Test
    void normalizeModelJsonPayload_stripsFenceAndFindsObject() {
        String raw =
                "```json\n{\"tasks\":[{\"title\":\"a\",\"description\":\"\",\"priority\":\"low\"}]}\n```";
        String n = TaskAiService.normalizeModelJsonPayload(raw);
        assertThat(n).startsWith("{").endsWith("}");
        assertThat(n).contains("\"tasks\"");
    }

    @Test
    void normalizeModelJsonPayload_ignoresProseBeforeJson() {
        String raw =
                "Here is the plan:\n{\"tasks\":[{\"title\":\"x\",\"description\":\"d\",\"priority\":\"medium\"}]}";
        String n = TaskAiService.normalizeModelJsonPayload(raw);
        assertThat(n).isEqualTo("{\"tasks\":[{\"title\":\"x\",\"description\":\"d\",\"priority\":\"medium\"}]}");
    }
}
