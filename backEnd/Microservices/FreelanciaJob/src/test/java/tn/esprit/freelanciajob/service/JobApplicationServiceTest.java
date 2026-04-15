package tn.esprit.freelanciajob.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.freelanciajob.Dto.request.JobApplicationRequest;
import tn.esprit.freelanciajob.Dto.response.ApplyJobResponse;
import tn.esprit.freelanciajob.Dto.response.AttachmentResponse;
import tn.esprit.freelanciajob.Dto.response.JobApplicationResponse;
import tn.esprit.freelanciajob.Entity.Job;
import tn.esprit.freelanciajob.Entity.JobApplication;
import tn.esprit.freelanciajob.Entity.ApplicationAttachment;
import tn.esprit.freelanciajob.Entity.Enums.ApplicationStatus;
import tn.esprit.freelanciajob.Entity.Enums.ClientType;
import tn.esprit.freelanciajob.Entity.Enums.LocationType;
import tn.esprit.freelanciajob.Event.ApplicationAcceptedEvent;
import tn.esprit.freelanciajob.Event.ApplicationSubmittedEvent;
import tn.esprit.freelanciajob.Repository.ApplicationAttachmentRepository;
import tn.esprit.freelanciajob.Repository.JobApplicationRepository;
import tn.esprit.freelanciajob.Repository.JobRepository;
import tn.esprit.freelanciajob.Service.FileStorageService;
import tn.esprit.freelanciajob.Service.JobApplicationServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JobApplicationServiceImpl}.
 *
 * Covers the full application lifecycle: add, update, delete, status transitions,
 * and the enhanced apply-with-attachments workflow.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JobApplicationService – Unit Tests")
class JobApplicationServiceTest {

    @Mock private JobApplicationRepository      applicationRepository;
    @Mock private JobRepository                 jobRepository;
    @Mock private ApplicationAttachmentRepository attachmentRepository;
    @Mock private FileStorageService            fileStorageService;
    @Mock private ApplicationEventPublisher     eventPublisher;

    @InjectMocks
    private JobApplicationServiceImpl applicationService;

    // ── Shared fixtures ───────────────────────────────────────────────────────

    private static final Long JOB_ID        = 10L;
    private static final Long FREELANCER_ID = 20L;
    private static final Long APP_ID        = 100L;

    private Job buildJob() {
        return Job.builder()
                .id(JOB_ID)
                .clientId(1L)
                .clientType(ClientType.INDIVIDUAL)
                .title("Backend Engineer")
                .description("Spring Boot microservices role.")
                .category("Engineering")
                .locationType(LocationType.REMOTE)
                .build();
    }

    private JobApplication buildApplication(ApplicationStatus status) {
        return JobApplication.builder()
                .id(APP_ID)
                .job(buildJob())
                .freelancerId(FREELANCER_ID)
                .proposalMessage("I am a great fit for this role with 5 years of experience.")
                .expectedRate(BigDecimal.valueOf(80))
                .status(status)
                .build();
    }

    private JobApplicationRequest buildRequest() {
        JobApplicationRequest r = new JobApplicationRequest();
        r.setJobId(JOB_ID);
        r.setFreelancerId(FREELANCER_ID);
        r.setProposalMessage("I am a great fit for this role with 5 years of experience.");
        r.setExpectedRate(BigDecimal.valueOf(80));
        r.setAvailabilityStart(LocalDate.now().plusDays(7));
        return r;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // addApplication()
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("addApplication()")
    class AddApplicationTests {

        @Test
        @DisplayName("should create PENDING application and publish ApplicationSubmittedEvent")
        void validRequest_createsApplicationAndPublishesEvent() {
            // Arrange
            when(applicationRepository.existsByJobIdAndFreelancerId(JOB_ID, FREELANCER_ID)).thenReturn(false);
            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(buildJob()));
            when(applicationRepository.save(any())).thenReturn(buildApplication(ApplicationStatus.PENDING));

            // Act
            JobApplicationResponse result = applicationService.addApplication(buildRequest());

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo("PENDING");
            verify(eventPublisher).publishEvent(any(ApplicationSubmittedEvent.class));
        }

        @Test
        @DisplayName("should throw RuntimeException when freelancer has already applied")
        void duplicateApplication_throwsRuntimeException() {
            // Arrange
            when(applicationRepository.existsByJobIdAndFreelancerId(JOB_ID, FREELANCER_ID)).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> applicationService.addApplication(buildRequest()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("already applied");

            verify(applicationRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("should throw RuntimeException when referenced job does not exist")
        void jobNotFound_throwsRuntimeException() {
            // Arrange
            when(applicationRepository.existsByJobIdAndFreelancerId(JOB_ID, FREELANCER_ID)).thenReturn(false);
            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> applicationService.addApplication(buildRequest()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining(JOB_ID.toString());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // updateApplication()
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updateApplication()")
    class UpdateApplicationTests {

        @Test
        @DisplayName("should update proposal and expected rate on existing application")
        void existingApp_fieldsUpdated() {
            // Arrange
            JobApplication existing = buildApplication(ApplicationStatus.PENDING);
            JobApplicationRequest req = buildRequest();
            req.setProposalMessage("Updated proposal with more details about my experience.");
            req.setExpectedRate(BigDecimal.valueOf(95));

            when(applicationRepository.findById(APP_ID)).thenReturn(Optional.of(existing));
            when(applicationRepository.save(any())).thenReturn(existing);

            // Act
            applicationService.updateApplication(APP_ID, req);

            // Assert
            verify(applicationRepository).save(argThat(a ->
                    "Updated proposal with more details about my experience.".equals(a.getProposalMessage())
                    && BigDecimal.valueOf(95).compareTo(a.getExpectedRate()) == 0
            ));
        }

        @Test
        @DisplayName("should throw RuntimeException when application does not exist")
        void unknownId_throwsRuntimeException() {
            // Arrange
            when(applicationRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> applicationService.updateApplication(999L, buildRequest()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("999");
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // deleteApplication()
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("deleteApplication()")
    class DeleteApplicationTests {

        @Test
        @DisplayName("should delete physical files, attachment records, and application row")
        void existingApp_cleansUpAllDataAndFiles() {
            // Arrange
            when(applicationRepository.existsById(APP_ID)).thenReturn(true);

            // Act
            applicationService.deleteApplication(APP_ID);

            // Assert – operations happen in the right order
            verify(fileStorageService).deleteApplicationFiles(APP_ID);
            verify(attachmentRepository).deleteByJobApplicationId(APP_ID);
            verify(applicationRepository).deleteById(APP_ID);
        }

        @Test
        @DisplayName("should throw RuntimeException when application does not exist")
        void unknownId_throwsRuntimeException() {
            // Arrange
            when(applicationRepository.existsById(999L)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> applicationService.deleteApplication(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("999");

            verify(fileStorageService, never()).deleteApplicationFiles(any());
            verify(applicationRepository, never()).deleteById(any());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // updateStatus()
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updateStatus()")
    class UpdateStatusTests {

        @Test
        @DisplayName("ACCEPTED status should persist change and publish ApplicationAcceptedEvent")
        void acceptStatus_persistsAndPublishesEvent() {
            // Arrange
            JobApplication app = buildApplication(ApplicationStatus.PENDING);
            when(applicationRepository.findById(APP_ID)).thenReturn(Optional.of(app));
            when(applicationRepository.save(any())).thenReturn(app);

            // Act
            applicationService.updateStatus(APP_ID, ApplicationStatus.ACCEPTED);

            // Assert
            verify(applicationRepository).save(argThat(a -> a.getStatus() == ApplicationStatus.ACCEPTED));
            verify(eventPublisher).publishEvent(any(ApplicationAcceptedEvent.class));
        }

        @Test
        @DisplayName("REJECTED status should persist change without publishing ApplicationAcceptedEvent")
        void rejectStatus_persistsWithoutAcceptEvent() {
            // Arrange
            JobApplication app = buildApplication(ApplicationStatus.PENDING);
            when(applicationRepository.findById(APP_ID)).thenReturn(Optional.of(app));
            when(applicationRepository.save(any())).thenReturn(app);

            // Act
            applicationService.updateStatus(APP_ID, ApplicationStatus.REJECTED);

            // Assert
            verify(applicationRepository).save(argThat(a -> a.getStatus() == ApplicationStatus.REJECTED));
            verify(eventPublisher, never()).publishEvent(any(ApplicationAcceptedEvent.class));
        }

        @Test
        @DisplayName("SHORTLISTED status should persist without any events")
        void shortlistStatus_persistsWithoutEvents() {
            // Arrange
            JobApplication app = buildApplication(ApplicationStatus.PENDING);
            when(applicationRepository.findById(APP_ID)).thenReturn(Optional.of(app));
            when(applicationRepository.save(any())).thenReturn(app);

            // Act
            applicationService.updateStatus(APP_ID, ApplicationStatus.SHORTLISTED);

            // Assert
            verify(applicationRepository).save(argThat(a -> a.getStatus() == ApplicationStatus.SHORTLISTED));
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("should throw RuntimeException when application does not exist")
        void unknownId_throwsRuntimeException() {
            // Arrange
            when(applicationRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> applicationService.updateStatus(999L, ApplicationStatus.ACCEPTED))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // applyToJob() — enhanced workflow with file attachments
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("applyToJob()")
    class ApplyToJobTests {

        @Test
        @DisplayName("should create application with no attachments when files list is null")
        void nullFiles_createsApplicationWithoutAttachments() {
            // Arrange
            when(applicationRepository.existsByJobIdAndFreelancerId(JOB_ID, FREELANCER_ID)).thenReturn(false);
            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(buildJob()));
            when(applicationRepository.save(any())).thenReturn(buildApplication(ApplicationStatus.PENDING));

            // Act
            ApplyJobResponse response = applicationService.applyToJob(
                    JOB_ID, FREELANCER_ID,
                    "I am a great fit for this role with 5 years of experience.",
                    BigDecimal.valueOf(80), LocalDate.now().plusDays(7), null);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getAttachments()).isEmpty();
            verify(fileStorageService, never()).validateFiles(any());
            verify(eventPublisher).publishEvent(any(ApplicationSubmittedEvent.class));
        }

        @Test
        @DisplayName("should validate and store files, create attachment records, and return them")
        void withFiles_storesAttachmentsAndReturnsMetadata() {
            // Arrange
            MultipartFile mockFile = mock(MultipartFile.class);
            when(mockFile.isEmpty()).thenReturn(false);
            when(mockFile.getOriginalFilename()).thenReturn("cv.pdf");
            when(mockFile.getContentType()).thenReturn("application/pdf");
            when(mockFile.getSize()).thenReturn(50_000L);

            when(applicationRepository.existsByJobIdAndFreelancerId(JOB_ID, FREELANCER_ID)).thenReturn(false);
            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(buildJob()));
            when(applicationRepository.save(any())).thenReturn(buildApplication(ApplicationStatus.PENDING));
            when(fileStorageService.storeFile(any(), any())).thenReturn("/uploads/cv-uuid.pdf");

            ApplicationAttachment savedAtt = ApplicationAttachment.builder()
                    .id(1L).jobApplicationId(APP_ID)
                    .fileName("cv.pdf").fileType("application/pdf")
                    .fileUrl("/uploads/cv-uuid.pdf").fileSize(50_000L)
                    .build();
            when(attachmentRepository.save(any())).thenReturn(savedAtt);

            // Act
            ApplyJobResponse response = applicationService.applyToJob(
                    JOB_ID, FREELANCER_ID,
                    "I am a great fit for this role with 5 years of experience.",
                    BigDecimal.valueOf(80), null, List.of(mockFile));

            // Assert
            assertThat(response.getAttachments()).hasSize(1);
            assertThat(response.getAttachments().get(0).getFileName()).isEqualTo("cv.pdf");
            verify(fileStorageService).validateFiles(anyList());
            verify(fileStorageService).storeFile(mockFile, APP_ID);
        }

        @Test
        @DisplayName("should throw RuntimeException when freelancer has already applied")
        void duplicateApply_throwsRuntimeException() {
            // Arrange
            when(applicationRepository.existsByJobIdAndFreelancerId(JOB_ID, FREELANCER_ID)).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> applicationService.applyToJob(
                    JOB_ID, FREELANCER_ID, "proposal", null, null, null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("already applied");

            verify(applicationRepository, never()).save(any());
        }

        @Test
        @DisplayName("should skip empty MultipartFile slots in the files list")
        void emptyFileInList_skippedAndNotStored() {
            // Arrange
            MultipartFile emptyFile = mock(MultipartFile.class);
            when(emptyFile.isEmpty()).thenReturn(true); // empty slot

            when(applicationRepository.existsByJobIdAndFreelancerId(JOB_ID, FREELANCER_ID)).thenReturn(false);
            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(buildJob()));
            when(applicationRepository.save(any())).thenReturn(buildApplication(ApplicationStatus.PENDING));

            // Act
            ApplyJobResponse response = applicationService.applyToJob(
                    JOB_ID, FREELANCER_ID, "proposal text here that is long enough.",
                    null, null, List.of(emptyFile));

            // Assert – empty file was filtered; no storage call made
            assertThat(response.getAttachments()).isEmpty();
            verify(fileStorageService, never()).storeFile(any(), any());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // getApplicationsByJob() and getApplicationsByFreelancer()
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getApplicationsByJob() – should return mapped DTOs for all applications")
    void getApplicationsByJob_returnsMappedDtos() {
        // Arrange
        when(applicationRepository.findByJobId(JOB_ID))
                .thenReturn(List.of(buildApplication(ApplicationStatus.PENDING)));

        // Act
        List<JobApplicationResponse> result = applicationService.getApplicationsByJob(JOB_ID);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getJobId()).isEqualTo(JOB_ID);
    }

    @Test
    @DisplayName("getApplicationsByFreelancer() – should return all applications for a freelancer")
    void getApplicationsByFreelancer_returnsMappedDtos() {
        // Arrange
        when(applicationRepository.findByFreelancerId(FREELANCER_ID))
                .thenReturn(List.of(buildApplication(ApplicationStatus.PENDING)));

        // Act
        List<JobApplicationResponse> result = applicationService.getApplicationsByFreelancer(FREELANCER_ID);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFreelancerId()).isEqualTo(FREELANCER_ID);
    }
}
