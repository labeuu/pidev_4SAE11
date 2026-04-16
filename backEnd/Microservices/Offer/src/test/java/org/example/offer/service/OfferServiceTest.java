package org.example.offer.service;

import org.example.offer.dto.request.OfferRequest;
import org.example.offer.dto.response.OfferResponse;
import org.example.offer.entity.Offer;
import org.example.offer.entity.OfferStatus;
import org.example.offer.exception.ResourceNotFoundException;
import org.example.offer.repository.OfferApplicationRepository;
import org.example.offer.repository.OfferRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class OfferServiceTest {

    @Mock
    OfferRepository offerRepository;
    @Mock
    OfferApplicationRepository applicationRepository;
    @Mock
    ModelMapper modelMapper;
    @Mock
    TranslationService translationService;

    @InjectMocks
    OfferService offerService;

    @Test
    void createOffer_clearsId_setsDraftAndDefaults() {
        OfferRequest request = new OfferRequest();
        request.setFreelancerId(10L);

        Offer mapped = new Offer();
        mapped.setId(5L);
        mapped.setFreelancerId(10L);
        mapped.setTitle("My offer title");
        mapped.setDomain("IT");
        mapped.setDescription("Description with enough chars here.");
        mapped.setPrice(new BigDecimal("100"));
        mapped.setDurationType("fixed");

        when(modelMapper.map(request, Offer.class)).thenReturn(mapped);
        when(offerRepository.save(any(Offer.class))).thenAnswer(inv -> {
            Offer o = inv.getArgument(0);
            assertThat(o.getId()).isNull();
            assertThat(o.getOfferStatus()).isEqualTo(OfferStatus.DRAFT);
            assertThat(o.getIsFeatured()).isFalse();
            assertThat(o.getIsActive()).isTrue();
            assertThat(o.getViewsCount()).isEqualTo(0);
            assertThat(o.getRating()).isEqualByComparingTo(BigDecimal.ZERO);
            Offer persisted = new Offer();
            persisted.setId(100L);
            persisted.setFreelancerId(o.getFreelancerId());
            persisted.setTitle(o.getTitle());
            persisted.setOfferStatus(o.getOfferStatus());
            return persisted;
        });

        OfferResponse mappedResp = new OfferResponse();
        mappedResp.setId(100L);
        when(modelMapper.map(any(Offer.class), eq(OfferResponse.class))).thenReturn(mappedResp);

        OfferResponse response = offerService.createOffer(request);

        assertThat(response.getId()).isEqualTo(100L);
        verify(offerRepository).save(any(Offer.class));
    }

    @Test
    void getOfferById_missing_throws() {
        when(offerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> offerService.getOfferById(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Offer not found");
    }

    @Test
    void getOfferById_incrementsViewsAndSaves() {
        Offer offer = new Offer();
        offer.setId(7L);
        offer.setFreelancerId(1L);
        offer.setTitle("Offer title");
        offer.setDomain("IT");
        offer.setDescription("Long enough description text.");
        offer.setPrice(new BigDecimal("50"));
        offer.setDurationType("hourly");
        offer.setOfferStatus(OfferStatus.AVAILABLE);
        offer.setIsActive(true);
        offer.setViewsCount(3);
        offer.setApplications(new java.util.ArrayList<>());

        when(offerRepository.findById(7L)).thenReturn(Optional.of(offer));
        when(offerRepository.save(offer)).thenReturn(offer);

        OfferResponse dto = new OfferResponse();
        dto.setId(7L);
        dto.setViewsCount(4);
        when(modelMapper.map(offer, OfferResponse.class)).thenReturn(dto);

        OfferResponse out = offerService.getOfferById(7L);

        assertThat(offer.getViewsCount()).isEqualTo(4);
        assertThat(out.getViewsCount()).isEqualTo(4);
        verify(offerRepository).save(offer);
    }
}
