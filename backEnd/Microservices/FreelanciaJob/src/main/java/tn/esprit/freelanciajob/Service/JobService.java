package tn.esprit.freelanciajob.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import tn.esprit.freelanciajob.Client.SkillClient;
import tn.esprit.freelanciajob.Client.UserClient;
import tn.esprit.freelanciajob.Dto.JobStats;
import tn.esprit.freelanciajob.Dto.Skills;
import tn.esprit.freelanciajob.Dto.request.JobRequest;
import tn.esprit.freelanciajob.Dto.request.JobSearchRequest;
import tn.esprit.freelanciajob.Dto.response.JobResponse;
import tn.esprit.freelanciajob.Dto.response.UserDto;
import tn.esprit.freelanciajob.Entity.Job;
import tn.esprit.freelanciajob.Entity.Enums.JobStatus;
import tn.esprit.freelanciajob.Event.JobCreatedEvent;
import tn.esprit.freelanciajob.Mapper.JobMapper;
import tn.esprit.freelanciajob.Repository.JobRepository;
import tn.esprit.freelanciajob.Specification.JobSpecification;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobService implements IJobService {

    private final JobRepository jobRepository;
    private final SkillClient skillClient;
    private final UserClient userClient;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Job addJob(JobRequest request) {
        Job job = JobMapper.toEntity(request);
        Job saved = jobRepository.save(job);

        // Resolve the client's display name (graceful fallback if USER service is down)
        String clientName = "A Client";
        if (request.getClientId() != null) {
            UserDto client = userClient.getUserById(request.getClientId());
            if (client != null && client.getFirstName() != null) {
                clientName = client.getFirstName() + " " +
                        (client.getLastName() != null ? client.getLastName() : "");
                clientName = clientName.trim();
            }
        }

        // Publish — decoupled from email logic
        eventPublisher.publishEvent(new JobCreatedEvent(this, saved, clientName));
        return saved;
    }

    @Override
    public Job updateJob(Long id, JobRequest request) {
        Job existing = jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + id));
        existing.setClientType(request.getClientType());
        existing.setCompanyName(request.getCompanyName());
        existing.setTitle(request.getTitle());
        existing.setDescription(request.getDescription());
        existing.setBudgetMin(request.getBudgetMin());
        existing.setBudgetMax(request.getBudgetMax());
        existing.setCurrency(request.getCurrency());
        existing.setDeadline(request.getDeadline());
        existing.setCategory(request.getCategory());
        existing.setLocationType(request.getLocationType());
        if (request.getRequiredSkillIds() != null) {
            existing.setRequiredSkillIds(request.getRequiredSkillIds());
        }
        return jobRepository.save(existing);
    }

    @Override
    public void deleteJob(Long id) {
        if (!jobRepository.existsById(id)) {
            throw new RuntimeException("Job not found with id: " + id);
        }
        jobRepository.deleteById(id);
    }

    @Override
    public Job getJobById(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + id));
    }

    @Override
    public JobResponse getJobResponse(Long id) {
        Job job = getJobById(id);
        return enrichWithSkills(job);
    }

    @Override
    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    @Override
    public List<JobResponse> getAllJobResponses() {
        return jobRepository.findAll().stream()
                .map(this::enrichWithSkills)
                .collect(Collectors.toList());
    }

    @Override
    public List<JobResponse> getJobsByClientId(Long clientId) {
        return jobRepository.findByClientId(clientId).stream()
                .map(this::enrichWithSkills)
                .collect(Collectors.toList());
    }

    @Override
    public List<JobResponse> getRecommendedJobs(Long freelancerId) {
        List<Skills> freelancerSkills = skillClient.getSkillsByUserId(freelancerId);
        Set<String> skillNames = freelancerSkills.stream()
                .map(Skills::getName)
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        List<Job> openJobs = jobRepository.findByStatus(JobStatus.OPEN);

        if (skillNames.isEmpty()) {
            return openJobs.stream()
                    .limit(6)
                    .map(this::enrichWithSkills)
                    .collect(Collectors.toList());
        }

        return openJobs.stream()
                .filter(job -> {
                    if (job.getRequiredSkillIds() == null || job.getRequiredSkillIds().isEmpty()) {
                        return false;
                    }
                    List<Skills> jobSkills = skillClient.getSkillsByIds(job.getRequiredSkillIds());
                    return jobSkills.stream()
                            .anyMatch(s -> s.getName() != null &&
                                    skillNames.contains(s.getName().toLowerCase()));
                })
                .limit(6)
                .map(this::enrichWithSkills)
                .collect(Collectors.toList());
    }

    @Override
    public List<JobResponse> searchJobs(String keyword, String category, BigDecimal budgetMin,
                                         BigDecimal budgetMax, String locationType, Long skillId) {
        tn.esprit.freelanciajob.Entity.Enums.LocationType locEnum = null;
        if (locationType != null && !locationType.trim().isEmpty()) {
            locEnum = tn.esprit.freelanciajob.Entity.Enums.LocationType.valueOf(locationType.toUpperCase());
        }
        List<Job> results = jobRepository.searchJobs(keyword, category, locEnum, budgetMin, budgetMax);

        if (skillId != null) {
            results = results.stream()
                    .filter(j -> j.getRequiredSkillIds() != null && j.getRequiredSkillIds().contains(skillId))
                    .collect(Collectors.toList());
        }

        return results.stream()
                .map(this::enrichWithSkills)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Long> getJobStatistics() {
        List<Job> allJobs = jobRepository.findAll();
        Map<String, Long> stats = new LinkedHashMap<>();
        for (JobStatus status : JobStatus.values()) {
            long count = allJobs.stream()
                    .filter(j -> j.getStatus() == status)
                    .count();
            stats.put(status.name(), count);
        }
        return stats;
    }

    @Override
    public List<JobStats> getJobsApplicationStats() {
        return jobRepository.getJobsStatistics();
    }

    @Override
    public Page<JobResponse> filterJobs(JobSearchRequest request) {
        Sort sort = "asc".equalsIgnoreCase(request.getSortDir())
                ? Sort.by(request.getSortBy()).ascending()
                : Sort.by(request.getSortBy()).descending();
        PageRequest pageable = PageRequest.of(request.getPage(), request.getSize(), sort);
        return jobRepository
                .findAll(JobSpecification.build(request), pageable)
                .map(this::enrichWithSkills);
    }

    private JobResponse enrichWithSkills(Job job) {
        JobResponse dto = JobMapper.toDto(job);
        if (job.getRequiredSkillIds() != null && !job.getRequiredSkillIds().isEmpty()) {
            List<Skills> skills = skillClient.getSkillsByIds(job.getRequiredSkillIds());
            dto.setSkills(skills);
        } else {
            dto.setSkills(Collections.emptyList());
        }
        return dto;
    }
}
