package tn.esprit.freelanciajob.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.freelanciajob.Dto.request.JobApplicationRequest;
import tn.esprit.freelanciajob.Dto.response.JobApplicationResponse;
import tn.esprit.freelanciajob.Entity.Job;
import tn.esprit.freelanciajob.Entity.JobApplication;
import tn.esprit.freelanciajob.Entity.Enums.ApplicationStatus;
import tn.esprit.freelanciajob.Mapper.JobMapper;
import tn.esprit.freelanciajob.Repository.JobApplicationRepository;
import tn.esprit.freelanciajob.Repository.JobRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobApplicationServiceImpl implements IJobApplicationService {

    private final JobApplicationRepository applicationRepository;
    private final JobRepository jobRepository;

    @Override
    public JobApplicationResponse addApplication(JobApplicationRequest request) {
        if (applicationRepository.existsByJobIdAndFreelancerId(request.getJobId(), request.getFreelancerId())) {
            throw new RuntimeException("Freelancer has already applied to this job");
        }
        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + request.getJobId()));

        JobApplication application = JobApplication.builder()
                .job(job)
                .freelancerId(request.getFreelancerId())
                .proposalMessage(request.getProposalMessage())
                .expectedRate(request.getExpectedRate())
                .availabilityStart(request.getAvailabilityStart())
                .status(ApplicationStatus.PENDING)
                .build();

        return JobMapper.toApplicationDto(applicationRepository.save(application));
    }

    @Override
    public JobApplicationResponse updateApplication(Long id, JobApplicationRequest request) {
        JobApplication existing = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found with id: " + id));
        existing.setProposalMessage(request.getProposalMessage());
        existing.setExpectedRate(request.getExpectedRate());
        existing.setAvailabilityStart(request.getAvailabilityStart());
        return JobMapper.toApplicationDto(applicationRepository.save(existing));
    }

    @Override
    public void deleteApplication(Long id) {
        if (!applicationRepository.existsById(id)) {
            throw new RuntimeException("Application not found with id: " + id);
        }
        applicationRepository.deleteById(id);
    }

    @Override
    public JobApplicationResponse getApplicationById(Long id) {
        JobApplication app = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found with id: " + id));
        return JobMapper.toApplicationDto(app);
    }

    @Override
    public List<JobApplicationResponse> getAllApplications() {
        return applicationRepository.findAll().stream()
                .map(JobMapper::toApplicationDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<JobApplicationResponse> getApplicationsByJob(Long jobId) {
        return applicationRepository.findByJobId(jobId).stream()
                .map(JobMapper::toApplicationDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<JobApplicationResponse> getApplicationsByFreelancer(Long freelancerId) {
        return applicationRepository.findByFreelancerId(freelancerId).stream()
                .map(JobMapper::toApplicationDto)
                .collect(Collectors.toList());
    }

    @Override
    public JobApplicationResponse updateStatus(Long id, ApplicationStatus status) {
        JobApplication app = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found with id: " + id));
        app.setStatus(status);
        return JobMapper.toApplicationDto(applicationRepository.save(app));
    }
}
