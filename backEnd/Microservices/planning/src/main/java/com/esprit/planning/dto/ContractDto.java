package com.esprit.planning.dto;

public class ContractDto {
    private Long id;
    private Long freelancerId;
    private String status;

    public ContractDto() {
    }

    public ContractDto(Long id, Long freelancerId, String status) {
        this.id = id;
        this.freelancerId = freelancerId;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFreelancerId() {
        return freelancerId;
    }

    public void setFreelancerId(Long freelancerId) {
        this.freelancerId = freelancerId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
