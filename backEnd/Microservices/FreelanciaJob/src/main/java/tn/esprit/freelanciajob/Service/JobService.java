package tn.esprit.freelanciajob.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.freelanciajob.Client.SkillClient;
import tn.esprit.freelanciajob.Dto.JobStats;
import tn.esprit.freelanciajob.Dto.Skills;
import tn.esprit.freelanciajob.Dto.request.JobRequest;
import tn.esprit.freelanciajob.Dto.response.JobResponse;
import tn.esprit.freelanciajob.Entity.Job;
import tn.esprit.freelanciajob.Entity.Enums.JobStatus;
import tn.esprit.freelanciajob.Mapper.JobMapper;
import tn.esprit.freelanciajob.Repository.JobRepository;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobService implements IJobService {

    private final JobRepository jobRepository;
    private final SkillClient skillClient;

    @Override
    public Job addJob(JobRequest request) {
        Job job = JobMapper.toEntity(request);
        return jobRepository.save(job);
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
        List<Job> results = jobRepository.searchJobs(keyword, category, locationType, budgetMin, budgetMax);

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
