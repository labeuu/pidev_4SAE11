package com.esprit.planning.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Single point in a progress trend time-series")
public class ProgressTrendPointDto {

    @Schema(description = "Date of the trend point")
    private LocalDate date;

    @Schema(description = "Progress percentage (e.g. latest or max for that day)")
    private Integer progressPercentage;

    public ProgressTrendPointDto() {}

    public ProgressTrendPointDto(LocalDate date, Integer progressPercentage) {
        this.date = date;
        this.progressPercentage = progressPercentage;
    }

    public LocalDate getDate() { return date; }
    public Integer getProgressPercentage() { return progressPercentage; }
    public void setDate(LocalDate date) { this.date = date; }
    public void setProgressPercentage(Integer progressPercentage) { this.progressPercentage = progressPercentage; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private LocalDate date;
        private Integer progressPercentage;

        public Builder date(LocalDate date) { this.date = date; return this; }
        public Builder progressPercentage(Integer progressPercentage) { this.progressPercentage = progressPercentage; return this; }

        public ProgressTrendPointDto build() {
            return new ProgressTrendPointDto(date, progressPercentage);
        }
    }
}
