package tn.esprit.freelanciajob.Mapper;

import tn.esprit.freelanciajob.Dto.request.JobRequest;
import tn.esprit.freelanciajob.Dto.response.JobApplicationResponse;
import tn.esprit.freelanciajob.Dto.response.JobResponse;
import tn.esprit.freelanciajob.Entity.Job;
import tn.esprit.freelanciajob.Entity.JobApplication;

import java.util.ArrayList;

public class JobMapper {

    private JobMapper() {}

    public static Job toEntity(JobRequest request) {
        Job job = new Job();
        job.setClientId(request.getClientId());
        job.setClientType(request.getClientType());
        job.setCompanyName(request.getCompanyName());
        job.setTitle(request.getTitle());
        job.setDescription(request.getDescription());
        job.setBudgetMin(request.getBudgetMin());
        job.setBudgetMax(request.getBudgetMax());
        job.setCurrency(request.getCurrency());
        job.setDeadline(request.getDeadline());
        job.setCategory(request.getCategory());
        job.setLocationType(request.getLocationType());
        if (request.getRequiredSkillIds() != null) {
            job.setRequiredSkillIds(request.getRequiredSkillIds());
        } else {
            job.setRequiredSkillIds(new ArrayList<>());
        }
        return job;
    }

    public static JobResponse toDto(Job job) {
        JobResponse dto = new JobResponse();
        dto.setId(job.getId());
        dto.setClientId(job.getClientId());
        dto.setClientType(job.getClientType() != null ? job.getClientType().name() : null);
        dto.setCompanyName(job.getCompanyName());
        dto.setTitle(job.getTitle());
        dto.setDescription(job.getDescription());
        dto.setBudgetMin(job.getBudgetMin());
        dto.setBudgetMax(job.getBudgetMax());
        dto.setCurrency(job.getCurrency());
        dto.setDeadline(job.getDeadline());
        dto.setCategory(job.getCategory());
        dto.setLocationType(job.getLocationType() != null ? job.getLocationType().name() : null);
        dto.setStatus(job.getStatus() != null ? job.getStatus().name() : null);
        dto.setRequiredSkillIds(job.getRequiredSkillIds());
        dto.setCreatedAt(job.getCreatedAt());
        dto.setUpdatedAt(job.getUpdatedAt());
        return dto;
    }

    public static JobApplicationResponse toApplicationDto(JobApplication app) {
        JobApplicationResponse dto = new JobApplicationResponse();
        dto.setId(app.getId());
        dto.setJobId(app.getJob() != null ? app.getJob().getId() : null);
        dto.setJobTitle(app.getJob() != null ? app.getJob().getTitle() : null);
        dto.setFreelancerId(app.getFreelancerId());
        dto.setProposalMessage(app.getProposalMessage());
        dto.setExpectedRate(app.getExpectedRate());
        dto.setAvailabilityStart(app.getAvailabilityStart());
        dto.setStatus(app.getStatus() != null ? app.getStatus().name() : null);
        dto.setCreatedAt(app.getCreatedAt());
        dto.setUpdatedAt(app.getUpdatedAt());
        return dto;
    }
}
