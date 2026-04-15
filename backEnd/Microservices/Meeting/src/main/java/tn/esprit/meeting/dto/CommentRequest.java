package tn.esprit.meeting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CommentRequest {

    @NotNull
    private Long userId;

    private String userName;

    @NotBlank
    private String content;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
