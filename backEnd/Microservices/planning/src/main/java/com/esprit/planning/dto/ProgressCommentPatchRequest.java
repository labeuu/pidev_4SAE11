package com.esprit.planning.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request body for partially updating a comment (PATCH)")
public class ProgressCommentPatchRequest {

    @Schema(description = "New comment text. If null, the message will not be changed.", example = "Updated comment after review.")
    private String message;

    public ProgressCommentPatchRequest() {}

    public ProgressCommentPatchRequest(String message) {
        this.message = message;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
