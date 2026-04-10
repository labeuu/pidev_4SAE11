package com.esprit.planning.dto;

import com.esprit.planning.entity.ProgressComment;
import com.esprit.planning.entity.ProgressUpdate;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Progress update along with its associated comments")
public class ProgressUpdateWithCommentsDto {

    @Schema(description = "The progress update")
    private ProgressUpdate progressUpdate;

    @Schema(description = "Comments attached to this progress update")
    private List<ProgressComment> comments;

    public ProgressUpdateWithCommentsDto() {}

    public ProgressUpdateWithCommentsDto(ProgressUpdate progressUpdate, List<ProgressComment> comments) {
        this.progressUpdate = progressUpdate;
        this.comments = comments;
    }

    public ProgressUpdate getProgressUpdate() { return progressUpdate; }
    public List<ProgressComment> getComments() { return comments; }
    public void setProgressUpdate(ProgressUpdate progressUpdate) { this.progressUpdate = progressUpdate; }
    public void setComments(List<ProgressComment> comments) { this.comments = comments; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private ProgressUpdate progressUpdate;
        private List<ProgressComment> comments;

        public Builder progressUpdate(ProgressUpdate progressUpdate) { this.progressUpdate = progressUpdate; return this; }
        public Builder comments(List<ProgressComment> comments) { this.comments = comments; return this; }

        public ProgressUpdateWithCommentsDto build() {
            return new ProgressUpdateWithCommentsDto(progressUpdate, comments);
        }
    }
}
