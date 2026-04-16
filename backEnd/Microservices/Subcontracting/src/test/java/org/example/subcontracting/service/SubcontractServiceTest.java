package org.example.subcontracting.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.subcontracting.client.NotificationFeignClient;
import org.example.subcontracting.client.OfferApplicationFeignClient;
import org.example.subcontracting.client.OfferFeignClient;
import org.example.subcontracting.client.ProjectFeignClient;
import org.example.subcontracting.client.UserFeignClient;
import org.example.subcontracting.dto.request.SubcontractRequest;
import org.example.subcontracting.entity.Subcontract;
import org.example.subcontracting.entity.SubcontractStatus;
import org.example.subcontracting.exception.BadRequestException;
import org.example.subcontracting.repository.SubcontractDeliverableRepository;
import org.example.subcontracting.repository.SubcontractRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubcontractServiceTest {

    @Mock
    SubcontractRepository subcontractRepo;
    @Mock
    SubcontractDeliverableRepository deliverableRepo;
    @Mock
    UserFeignClient userClient;
    @Mock
    ProjectFeignClient projectClient;
    @Mock
    OfferApplicationFeignClient offerApplicationClient;
    @Mock
    OfferFeignClient offerClient;
    @Mock
    NotificationFeignClient notificationClient;
    @Mock
    SubcontractAuditService auditService;
    @Mock
    SubcontractEmailService subcontractEmailService;
    @Mock
    SubcontractCoachingService coachingService;
    @Mock
    ObjectMapper objectMapper;

    @InjectMocks
    SubcontractService subcontractService;

    @Test
    void create_throwsWhenSelfSubcontract() {
        SubcontractRequest req = new SubcontractRequest();
        req.setSubcontractorId(5L);
        req.setProjectId(1L);

        assertThatThrownBy(() -> subcontractService.create(5L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("soi-même");

        verifyNoInteractions(userClient, projectClient, offerApplicationClient, offerClient, notificationClient);
        verifyNoInteractions(auditService);
    }

    @Test
    void create_throwsWhenNeitherProjectNorOffer() {
        SubcontractRequest req = new SubcontractRequest();
        req.setSubcontractorId(2L);
        req.setProjectId(null);
        req.setOfferId(null);

        assertThatThrownBy(() -> subcontractService.create(1L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("exactement une mission");

        verifyNoInteractions(userClient, projectClient, offerApplicationClient, offerClient);
    }

    @Test
    void create_throwsWhenBothProjectAndOffer() {
        SubcontractRequest req = new SubcontractRequest();
        req.setSubcontractorId(2L);
        req.setProjectId(10L);
        req.setOfferId(20L);

        assertThatThrownBy(() -> subcontractService.create(1L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("exactement une mission");

        verifyNoInteractions(userClient, projectClient, offerApplicationClient, offerClient);
    }

    @Test
    void update_throwsWhenNotDraft() {
        Subcontract sc = new Subcontract();
        sc.setId(99L);
        sc.setStatus(SubcontractStatus.PROPOSED);
        when(subcontractRepo.findById(99L)).thenReturn(Optional.of(sc));

        SubcontractRequest req = new SubcontractRequest();

        assertThatThrownBy(() -> subcontractService.update(99L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("DRAFT");

        verifyNoInteractions(auditService);
    }
}
