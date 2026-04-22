package org.example.subcontracting.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.subcontracting.client.NotificationFeignClient;
import org.example.subcontracting.client.OfferApplicationFeignClient;
import org.example.subcontracting.client.OfferFeignClient;
import org.example.subcontracting.client.ProjectFeignClient;
import org.example.subcontracting.client.UserFeignClient;
import org.example.subcontracting.dto.request.CounterOfferRequest;
import org.example.subcontracting.dto.request.SubcontractRequest;
import org.example.subcontracting.dto.response.NegotiationRoundResponse;
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
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
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

    @Test
    void counterOffer_throwsWhenSubcontractorMismatch() {
        Subcontract sc = new Subcontract();
        sc.setId(10L);
        sc.setSubcontractorId(50L);
        sc.setStatus(SubcontractStatus.PROPOSED);
        sc.setNegotiationRoundCount(0);
        when(subcontractRepo.findById(10L)).thenReturn(Optional.of(sc));

        CounterOfferRequest req = new CounterOfferRequest(new BigDecimal("200"), 12, "new proposal");

        assertThatThrownBy(() -> subcontractService.counterOffer(10L, 99L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Seul le sous-traitant assigné");
    }

    @Test
    void counterOffer_throwsWhenMaxRoundsReached() {
        Subcontract sc = new Subcontract();
        sc.setId(11L);
        sc.setSubcontractorId(5L);
        sc.setStatus(SubcontractStatus.COUNTER_OFFERED);
        sc.setNegotiationRoundCount(3);
        when(subcontractRepo.findById(11L)).thenReturn(Optional.of(sc));
        when(subcontractRepo.save(any(Subcontract.class))).thenAnswer(inv -> inv.<Subcontract>getArgument(0));

        CounterOfferRequest req = new CounterOfferRequest(new BigDecimal("250"), 10, "round 4");

        assertThatThrownBy(() -> subcontractService.counterOffer(11L, 5L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Maximum 3 rounds atteint");

        verify(subcontractRepo).save(org.mockito.ArgumentMatchers.argThat(java.util.Objects::nonNull));
        assertThat(sc.getNegotiationStatus()).isEqualTo("NEGOTIATION_IMPASSE");
    }

    @Test
    void counterOffer_success_setsCounterDataAndReturnsRoundResponse() {
        Subcontract sc = new Subcontract();
        sc.setId(12L);
        sc.setMainFreelancerId(100L);
        sc.setSubcontractorId(5L);
        sc.setTitle("API delivery");
        sc.setStatus(SubcontractStatus.PROPOSED);
        sc.setNegotiationRoundCount(0);
        sc.setBudget(new BigDecimal("300"));
        sc.setStartDate(LocalDate.now());
        sc.setDeadline(LocalDate.now().plusDays(10));
        when(subcontractRepo.findById(12L)).thenReturn(Optional.of(sc));
        when(subcontractRepo.save(any(Subcontract.class))).thenAnswer(inv -> inv.<Subcontract>getArgument(0));

        CounterOfferRequest req = new CounterOfferRequest(new BigDecimal("350"), 14, "need more scope budget");

        NegotiationRoundResponse out = subcontractService.counterOffer(12L, 5L, req);

        assertThat(sc.getStatus()).isEqualTo(SubcontractStatus.COUNTER_OFFERED);
        assertThat(sc.getNegotiationRoundCount()).isEqualTo(1);
        assertThat(sc.getNegotiationStatus()).isEqualTo("COUNTER_OFFERED");
        assertThat(sc.getCounterOfferBudget()).isEqualByComparingTo("350");
        assertThat(sc.getCounterOfferDurationDays()).isEqualTo(14);
        assertThat(out.getRoundNumber()).isEqualTo(1);
        assertThat(out.getNegotiationStatus()).isEqualTo("COUNTER_OFFERED");
    }
}
