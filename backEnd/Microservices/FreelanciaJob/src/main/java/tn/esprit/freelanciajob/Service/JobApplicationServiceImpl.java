package tn.esprit.freelanciajob.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.freelanciajob.Dto.request.JobApplicationRequest;
import tn.esprit.freelanciajob.Client.UserClient;
import tn.esprit.freelanciajob.Dto.response.ApplyJobResponse;
import tn.esprit.freelanciajob.Dto.response.AttachmentResponse;
import tn.esprit.freelanciajob.Dto.response.JobApplicationResponse;
import tn.esprit.freelanciajob.Dto.response.UserDto;
import tn.esprit.freelanciajob.Entity.ApplicationAttachment;
import tn.esprit.freelanciajob.Entity.Job;
import tn.esprit.freelanciajob.Entity.JobApplication;
import tn.esprit.freelanciajob.Entity.Enums.ApplicationStatus;
import tn.esprit.freelanciajob.Event.ApplicationAcceptedEvent;
import tn.esprit.freelanciajob.Event.ApplicationSubmittedEvent;
import tn.esprit.freelanciajob.Mapper.JobMapper;
import tn.esprit.freelanciajob.Repository.ApplicationAttachmentRepository;
import tn.esprit.freelanciajob.Repository.JobApplicationRepository;
import tn.esprit.freelanciajob.Repository.JobRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobApplicationServiceImpl implements IJobApplicationService {

    private final JobApplicationRepository      applicationRepository;
    private final JobRepository                 jobRepository;
    private final ApplicationAttachmentRepository attachmentRepository;
    private final FileStorageService            fileStorageService;
    private final ApplicationEventPublisher     eventPublisher;
    private final UserClient                    userClient;

    // ── Existing CRUD (unchanged behaviour) ──────────────────────────────────

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

        JobApplication saved = applicationRepository.save(application);
        initializeJobForAsyncEvents(saved);
        eventPublisher.publishEvent(new ApplicationSubmittedEvent(this, saved, request.getFreelancerId()));
        return enrichFreelancerDisplay(JobMapper.toApplicationDto(saved));
    }

    @Override
    public JobApplicationResponse updateApplication(Long id, JobApplicationRequest request) {
        JobApplication existing = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found with id: " + id));
        existing.setProposalMessage(request.getProposalMessage());
        existing.setExpectedRate(request.getExpectedRate());
        existing.setAvailabilityStart(request.getAvailabilityStart());
        return enrichFreelancerDisplay(JobMapper.toApplicationDto(applicationRepository.save(existing)));
    }

    @Override
    public void deleteApplication(Long id) {
        if (!applicationRepository.existsById(id)) {
            throw new RuntimeException("Application not found with id: " + id);
        }
        // Clean up physical files before removing the DB record
        fileStorageService.deleteApplicationFiles(id);
        attachmentRepository.deleteByJobApplicationId(id);
        applicationRepository.deleteById(id);
    }

    @Override
    public JobApplicationResponse getApplicationById(Long id) {
        JobApplication app = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found with id: " + id));
        return enrichFreelancerDisplay(JobMapper.toApplicationDto(app));
    }

    @Override
    public List<JobApplicationResponse> getAllApplications() {
        return applicationRepository.findAll().stream()
                .map(JobMapper::toApplicationDto)
                .map(this::enrichFreelancerDisplay)
                .collect(Collectors.toList());
    }

    @Override
    public List<JobApplicationResponse> getApplicationsByJob(Long jobId) {
        return applicationRepository.findByJobId(jobId).stream()
                .map(JobMapper::toApplicationDto)
                .map(this::enrichFreelancerDisplay)
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
        JobApplication saved = applicationRepository.save(app);

        if (status == ApplicationStatus.ACCEPTED) {
            initializeJobForAsyncEvents(saved);
            eventPublisher.publishEvent(new ApplicationAcceptedEvent(this, saved, saved.getFreelancerId()));
        }
        return enrichFreelancerDisplay(JobMapper.toApplicationDto(saved));
    }

    // ── New: enhanced apply workflow with attachments ─────────────────────────

    @Override
    @Transactional
    public ApplyJobResponse applyToJob(Long jobId, Long freelancerId, String proposalMessage,
                                       BigDecimal expectedRate, LocalDate availabilityStart,
                                       List<MultipartFile> files) {

        if (applicationRepository.existsByJobIdAndFreelancerId(jobId, freelancerId)) {
            throw new RuntimeException("Freelancer has already applied to this job.");
        }

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));

        // 1 ── Persist application
        JobApplication application = JobApplication.builder()
                .job(job)
                .freelancerId(freelancerId)
                .proposalMessage(proposalMessage)
                .expectedRate(expectedRate)
                .availabilityStart(availabilityStart)
                .status(ApplicationStatus.PENDING)
                .build();
        application = applicationRepository.save(application);
        initializeJobForAsyncEvents(application);

        // 2 ── Handle attachments (skip empty multipart slots)
        List<MultipartFile> nonEmpty = (files == null) ? Collections.emptyList() :
                files.stream().filter(f -> f != null && !f.isEmpty()).collect(Collectors.toList());

        List<AttachmentResponse> attachments = new ArrayList<>();
        if (!nonEmpty.isEmpty()) {
            fileStorageService.validateFiles(nonEmpty);
            for (MultipartFile file : nonEmpty) {
                String fileUrl = fileStorageService.storeFile(file, application.getId());
                ApplicationAttachment att = ApplicationAttachment.builder()
                        .jobApplicationId(application.getId())
                        .fileName(file.getOriginalFilename())
                        .fileType(file.getContentType())
                        .fileUrl(fileUrl)
                        .fileSize(file.getSize())
                        .build();
                attachments.add(toAttachmentResponse(attachmentRepository.save(att)));
            }
        }

        // 3 ── Publish event (triggers email to freelancer + client via listener)
        eventPublisher.publishEvent(new ApplicationSubmittedEvent(this, application, freelancerId));

        // 4 ── Build and return response
        return buildApplyJobResponse(application, attachments);
    }

    @Override
    public List<AttachmentResponse> getAttachments(Long applicationId) {
        return attachmentRepository.findByJobApplicationId(applicationId)
                .stream()
                .map(this::toAttachmentResponse)
                .collect(Collectors.toList());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Loads the lazy {@link Job} inside the transaction so {@link tn.esprit.freelanciajob.Event.ApplicationSubmittedEvent}
     * listeners (async email) can safely read job title and client id after the persistence context closes.
     */
    private void initializeJobForAsyncEvents(JobApplication application) {
        Job job = application.getJob();
        if (job != null) {
            job.getId();
            job.getTitle();
            job.getClientId();
        }
    }

    /**
     * Fills {@code freelancerFirstName} / {@code freelancerLastName} from the USER microservice
     * for API consumers (e.g. job owner viewing applicants).
     */
    private JobApplicationResponse enrichFreelancerDisplay(JobApplicationResponse dto) {
        if (dto == null || dto.getFreelancerId() == null) {
            return dto;
        }
        try {
            UserDto u = userClient.getUserById(dto.getFreelancerId());
            if (u != null) {
                dto.setFreelancerFirstName(u.getFirstName());
                dto.setFreelancerLastName(u.getLastName());
            }
        } catch (Exception e) {
            log.debug("Could not load freelancer display name for id {}: {}", dto.getFreelancerId(), e.getMessage());
        }
        return dto;
    }

    private AttachmentResponse toAttachmentResponse(ApplicationAttachment att) {
        AttachmentResponse r = new AttachmentResponse();
        r.setId(att.getId());
        r.setJobApplicationId(att.getJobApplicationId());
        r.setFileName(att.getFileName());
        r.setFileType(att.getFileType());
        r.setFileUrl(att.getFileUrl());
        r.setFileSize(att.getFileSize());
        r.setUploadedAt(att.getUploadedAt());
        return r;
    }

    private ApplyJobResponse buildApplyJobResponse(JobApplication application,
                                                    List<AttachmentResponse> attachments) {
        ApplyJobResponse r = new ApplyJobResponse();
        r.setId(application.getId());
        r.setJobId(application.getJob().getId());
        r.setJobTitle(application.getJob().getTitle());
        r.setFreelancerId(application.getFreelancerId());
        r.setProposalMessage(application.getProposalMessage());
        r.setExpectedRate(application.getExpectedRate());
        r.setAvailabilityStart(application.getAvailabilityStart());
        r.setStatus(application.getStatus().name());
        r.setCreatedAt(application.getCreatedAt());
        r.setAttachments(attachments);
        return r;
    }
}
