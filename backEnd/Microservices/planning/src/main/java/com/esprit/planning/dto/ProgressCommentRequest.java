package com.esprit.planning.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request body for creating a comment or updating comment message")
public class ProgressCommentRequest {

    @Schema(description = "ID of the progress update this comment belongs to", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long progressUpdateId;

    @Schema(description = "ID of the user posting the comment", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long userId;

    @Schema(description = "Comment text", example = "Great progress! Please add unit tests for the new endpoints.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String message;

    public ProgressCommentRequest() {}

    public ProgressCommentRequest(Long progressUpdateId, Long userId, String message) {
        this.progressUpdateId = progressUpdateId;
        this.userId = userId;
        this.message = message;
    }

    public Long getProgressUpdateId() { return progressUpdateId; }
    public Long getUserId() { return userId; }
    public String getMessage() { return message; }

    public void setProgressUpdateId(Long progressUpdateId) { this.progressUpdateId = progressUpdateId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setMessage(String message) { this.message = message; }
}
