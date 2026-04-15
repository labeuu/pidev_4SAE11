package com.esprit.planning.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectApplicationFeignDto {
    private Long id;
    private Long freelanceId;
    private String status;
    private NestedProject project;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NestedProject {
        private Long id;
        private String title;

        public NestedProject() {
        }

        public NestedProject(Long id, String title) {
            this.id = id;
            this.title = title;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }

    public ProjectApplicationFeignDto() {
    }

    public ProjectApplicationFeignDto(Long id, Long freelanceId, String status, NestedProject project) {
        this.id = id;
        this.freelanceId = freelanceId;
        this.status = status;
        this.project = project;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFreelanceId() {
        return freelanceId;
    }

    public void setFreelanceId(Long freelanceId) {
        this.freelanceId = freelanceId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public NestedProject getProject() {
        return project;
    }

    public void setProject(NestedProject project) {
        this.project = project;
    }
}
