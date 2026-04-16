package org.example.offer.service;

import org.example.offer.client.ContractClient;
import org.example.offer.client.ContractCreateRequest;
import org.example.offer.dto.request.OfferApplicationRequest;
import org.example.offer.dto.response.OfferApplicationResponse;
import org.example.offer.entity.ApplicationStatus;
import org.example.offer.entity.Offer;
import org.example.offer.entity.OfferApplication;
import org.example.offer.entity.OfferStatus;
import org.example.offer.exception.BadRequestException;
import org.example.offer.exception.ResourceNotFoundException;
import org.example.offer.repository.OfferApplicationRepository;
import org.example.offer.repository.OfferRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OfferApplicationServiceTest {

    @Mock
    OfferApplicationRepository applicationRepository;
    @Mock
    OfferRepository offerRepository;
    @Mock
    ModelMapper modelMapper;
    @Mock
    ContractClient contractClient;

    @InjectMocks
    OfferApplicationService offerApplicationService;

    private static Offer buildOpenOffer(Long id, Long freelancerId) {
        Offer offer = new Offer();
        offer.setId(id);
        offer.setFreelancerId(freelancerId);
        offer.setTitle("Valid offer title");
        offer.setDomain("IT");
        offer.setDescription("Valid description with enough length.");
        offer.setPrice(new BigDecimal("100"));
        offer.setDurationType("fixed");
        offer.setOfferStatus(OfferStatus.AVAILABLE);
        offer.setIsActive(true);
        offer.setApplications(new java.util.ArrayList<>());
        return offer;
    }

    @Test
    void applyToOffer_offerNotFound_throws() {
        OfferApplicationRequest req = new OfferApplicationRequest(1L, 2L, "Message with twenty chars+", new BigDecimal("10"), null, null, null);
        when(offerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> offerApplicationService.applyToOffer(req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Offer not found");
    }

    @Test
    void applyToOffer_notAcceptingApplications_throws() {
        Offer offer = buildOpenOffer(1L, 10L);
        offer.setIsActive(false);
        OfferApplicationRequest req = new OfferApplicationRequest(1L, 20L, "Message with twenty chars+", new BigDecimal("10"), null, null, null);
        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> offerApplicationService.applyToOffer(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not accepting applications");
    }

    @Test
    void applyToOffer_clientIsFreelancer_throws() {
        Offer offer = buildOpenOffer(1L, 10L);
        OfferApplicationRequest req = new OfferApplicationRequest(1L, 10L, "Message with twenty chars+", new BigDecimal("10"), null, null, null);
        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> offerApplicationService.applyToOffer(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("own offer");
    }

    @Test
    void applyToOffer_duplicateApplication_throws() {
        Offer offer = buildOpenOffer(1L, 10L);
        OfferApplicationRequest req = new OfferApplicationRequest(1L, 20L, "Message with twenty chars+", new BigDecimal("10"), null, null, null);
        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(applicationRepository.existsByOfferIdAndClientId(1L, 20L)).thenReturn(true);

        assertThatThrownBy(() -> offerApplicationService.applyToOffer(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already applied");
    }

    @Test
    void applyToOffer_success_savesApplication() {
        Offer offer = buildOpenOffer(1L, 10L);
        OfferApplicationRequest req = new OfferApplicationRequest(1L, 20L, "Message with twenty chars+", new BigDecimal("10"), null, null, null);
        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(applicationRepository.existsByOfferIdAndClientId(1L, 20L)).thenReturn(false);
        when(applicationRepository.save(any(OfferApplication.class))).thenAnswer(inv -> {
            OfferApplication app = inv.getArgument(0);
            app.setId(55L);
            return app;
        });

        OfferApplicationResponse mapped = new OfferApplicationResponse();
        mapped.setId(55L);
        when(modelMapper.map(any(OfferApplication.class), eq(OfferApplicationResponse.class))).thenReturn(mapped);

        OfferApplicationResponse response = offerApplicationService.applyToOffer(req);

        assertThat(response.getId()).isEqualTo(55L);
        ArgumentCaptor<OfferApplication> captor = ArgumentCaptor.forClass(OfferApplication.class);
        verify(applicationRepository).save(captor.capture());
        OfferApplication saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(ApplicationStatus.PENDING);
        assertThat(saved.getClientId()).isEqualTo(20L);
        assertThat(saved.getOffer()).isSameAs(offer);
    }

    @Test
    void acceptApplication_wrongFreelancer_throws() {
        Offer offer = buildOpenOffer(1L, 10L);
        OfferApplication app = new OfferApplication();
        app.setId(3L);
        app.setOffer(offer);
        app.setClientId(20L);
        app.setMessage("Message with twenty chars+");
        app.setProposedBudget(new BigDecimal("50"));
        app.setStatus(ApplicationStatus.PENDING);

        when(applicationRepository.findById(3L)).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> offerApplicationService.acceptApplication(3L, 99L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not authorized");
    }

    @Test
    void acceptApplication_success_callsContractClient() {
        Offer offer = buildOpenOffer(1L, 10L);
        offer.setTitle("Build API");
        offer.setDescription("Desc");
        offer.setDeadline(java.time.LocalDate.now().plusDays(30));

        OfferApplication app = new OfferApplication();
        app.setId(3L);
        app.setOffer(offer);
        app.setClientId(20L);
        app.setMessage("Message with twenty chars+");
        app.setProposedBudget(new BigDecimal("75"));
        app.setStatus(ApplicationStatus.PENDING);

        when(applicationRepository.findById(3L)).thenReturn(Optional.of(app));
        when(applicationRepository.save(any(OfferApplication.class))).thenAnswer(inv -> inv.getArgument(0));
        when(offerRepository.save(any(Offer.class))).thenAnswer(inv -> inv.getArgument(0));

        OfferApplicationResponse mapped = new OfferApplicationResponse();
        when(modelMapper.map(any(OfferApplication.class), eq(OfferApplicationResponse.class))).thenReturn(mapped);

        offerApplicationService.acceptApplication(3L, 10L);

        ArgumentCaptor<ContractCreateRequest> contractCaptor = ArgumentCaptor.forClass(ContractCreateRequest.class);
        verify(contractClient).createContractFromAcceptedApplication(contractCaptor.capture());
        ContractCreateRequest cr = contractCaptor.getValue();
        assertThat(cr.getClientId()).isEqualTo(20L);
        assertThat(cr.getFreelancerId()).isEqualTo(10L);
        assertThat(cr.getOfferApplicationId()).isEqualTo(3L);
        assertThat(cr.getAmount()).isEqualByComparingTo(new BigDecimal("75"));
    }
}
