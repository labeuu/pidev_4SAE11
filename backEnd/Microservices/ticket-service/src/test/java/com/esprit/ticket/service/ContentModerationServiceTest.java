package com.esprit.ticket.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentModerationServiceTest {

    @Mock
    private org.springframework.web.client.RestTemplate restTemplate;

    private ContentModerationService contentModerationService;

    @BeforeEach
    void setUp() {
        contentModerationService = new ContentModerationService(restTemplate);
        ReflectionTestUtils.setField(contentModerationService, "moderationBaseUrl", "https://www.purgomalum.com/service");
        ReflectionTestUtils.setField(contentModerationService, "moderationEnabled", true);
        ReflectionTestUtils.setField(contentModerationService, "rejectOnProfanity", true);
    }

    @Test
    void validateAndPrepareText_cleanText_returnsInput() {
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("false"));

        String out = contentModerationService.validateAndPrepareText("hello clean world");

        assertThat(out).isEqualTo("hello clean world");
    }

    @Test
    void validateAndPrepareText_profanityAndRejectEnabled_throwsBadRequest() {
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("true"));

        assertThatThrownBy(() -> contentModerationService.validateAndPrepareText("bad words"))
                .isInstanceOf(ResponseStatusException.class)
                .matches(ex -> ((ResponseStatusException) ex).getStatusCode().value() == 400);
    }

    @Test
    void validateAndPrepareText_profanityAndRejectDisabled_returnsCensoredText() {
        ReflectionTestUtils.setField(contentModerationService, "rejectOnProfanity", false);

        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("true"))
                .thenReturn(ResponseEntity.ok("cleaned text"));

        String out = contentModerationService.validateAndPrepareText("bad words");

        assertThat(out).isEqualTo("cleaned text");
    }

    @Test
    void containsProfanity_non2xxResponse_treatedAsClean() {
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("error"));

        boolean result = contentModerationService.containsProfanity("some text");

        assertThat(result).isFalse();
    }
}
