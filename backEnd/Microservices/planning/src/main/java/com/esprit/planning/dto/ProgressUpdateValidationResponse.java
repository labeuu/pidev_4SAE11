package com.esprit.planning.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Response payload for validating a progress update without persisting it")
public class ProgressUpdateValidationResponse {

    @Schema(description = "Whether the provided progress update is valid according to current rules")
    private boolean valid;

    @Schema(description = "Minimum allowed progress percentage for the project, considering existing updates")
    private Integer minAllowed;

    @Schema(description = "Progress percentage provided by the client for this validation")
    private Integer provided;

    @Schema(description = "List of validation error messages (empty when valid = true)")
    private List<String> errors;

    public ProgressUpdateValidationResponse() {}

    public ProgressUpdateValidationResponse(boolean valid, Integer minAllowed, Integer provided, List<String> errors) {
        this.valid = valid;
        this.minAllowed = minAllowed;
        this.provided = provided;
        this.errors = errors;
    }

    public boolean isValid() { return valid; }
    public Integer getMinAllowed() { return minAllowed; }
    public Integer getProvided() { return provided; }
    public List<String> getErrors() { return errors; }

    public void setValid(boolean valid) { this.valid = valid; }
    public void setMinAllowed(Integer minAllowed) { this.minAllowed = minAllowed; }
    public void setProvided(Integer provided) { this.provided = provided; }
    public void setErrors(List<String> errors) { this.errors = errors; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private boolean valid;
        private Integer minAllowed;
        private Integer provided;
        private List<String> errors;

        public Builder valid(boolean valid) { this.valid = valid; return this; }
        public Builder minAllowed(Integer minAllowed) { this.minAllowed = minAllowed; return this; }
        public Builder provided(Integer provided) { this.provided = provided; return this; }
        public Builder errors(List<String> errors) { this.errors = errors; return this; }

        public ProgressUpdateValidationResponse build() {
            return new ProgressUpdateValidationResponse(valid, minAllowed, provided, errors);
        }
    }
}
