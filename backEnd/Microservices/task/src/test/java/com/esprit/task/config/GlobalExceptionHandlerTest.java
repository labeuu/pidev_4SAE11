package com.esprit.task.config;

import com.esprit.task.exception.EntityNotFoundException;
import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleEntityNotFound_returns404WithMessage() {
        EntityNotFoundException ex = new EntityNotFoundException("Task", 999L);

        ResponseEntity<Map<String, String>> result = handler.handleEntityNotFound(ex);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(result.getBody()).containsKey("message");
        assertThat(result.getBody().get("message")).contains("Task not found");
        assertThat(result.getBody().get("message")).contains("999");
    }

    @Test
    void handleIllegalArgument_returns400WithMessage() {
        ResponseEntity<Map<String, String>> result =
                handler.handleIllegalArgument(new IllegalArgumentException("taskIds is required"));

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(result.getBody()).containsEntry("message", "taskIds is required");
    }

    @Test
    void handleIllegalArgument_whenMessageNull_usesFallback() {
        ResponseEntity<Map<String, String>> result =
                handler.handleIllegalArgument(new IllegalArgumentException());

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(result.getBody()).containsEntry("message", "Bad request");
    }

    @Test
    void handleUnreadableJson_returns400() {
        ResponseEntity<Map<String, String>> result =
                handler.handleUnreadableJson(new HttpMessageNotReadableException("broken", null));

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(result.getBody()).containsEntry("message", "Invalid or missing JSON body");
    }

    @Test
    void handleResponseStatus_returns403() {
        ResponseEntity<Map<String, String>> result =
                handler.handleResponseStatus(new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed"));

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(result.getBody()).containsEntry("message", "Not allowed");
    }

    @Test
    void handleFeign_maps503AndForwardsAImodelErrorMessage() {
        FeignException ex = mock(FeignException.class);
        when(ex.status()).thenReturn(503);
        when(ex.contentUTF8())
                .thenReturn("{\"success\":false,\"error\":{\"message\":\"Ollama server is unavailable\"}}");
        when(ex.getMessage()).thenReturn("503 Service Unavailable");

        ResponseEntity<Map<String, String>> result = handler.handleFeign(ex);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(result.getBody()).containsEntry("message", "Ollama server is unavailable");
    }

    @Test
    void resolveFeignClientMessage_whenEmptyBodyAndConnectionRefused() {
        FeignException ex = mock(FeignException.class);
        when(ex.status()).thenReturn(-1);
        when(ex.contentUTF8()).thenReturn("");
        when(ex.getMessage()).thenReturn("Connection refused: connect");

        assertThat(GlobalExceptionHandler.resolveFeignClientMessage(ex))
                .contains("Connection refused")
                .contains("Project and AIMODEL services")
                .contains("microservices are running");
    }

    @Test
    void handleValidation_mapsFieldErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult br = mock(BindingResult.class);
        FieldError fe = new FieldError("body", "title", "must not be blank");
        when(ex.getBindingResult()).thenReturn(br);
        when(br.getFieldErrors()).thenReturn(List.of(fe));

        ResponseEntity<Map<String, Object>> result = handler.handleValidation(ex);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(result.getBody()).containsKey("errors");
        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) result.getBody().get("errors");
        assertThat(errors).containsEntry("title", "must not be blank");
    }

    @Test
    void handleFeign_408_mapsToGatewayTimeout() {
        FeignException ex = mock(FeignException.class);
        when(ex.status()).thenReturn(408);
        when(ex.contentUTF8()).thenReturn("");
        when(ex.getMessage()).thenReturn("timeout");

        ResponseEntity<Map<String, String>> result = handler.handleFeign(ex);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
    }

    @Test
    void handleResponseStatus_whenReasonNull_usesStatusPhrase() {
        ResponseEntity<Map<String, String>> result =
                handler.handleResponseStatus(new ResponseStatusException(HttpStatus.NOT_FOUND));

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(result.getBody()).containsKey("message");
        assertThat(result.getBody().get("message")).isNotBlank();
    }

    @Test
    void resolveFeignClientMessage_whenMessageContainsTimeout_hint() {
        FeignException ex = mock(FeignException.class);
        when(ex.contentUTF8()).thenReturn("");
        when(ex.getMessage()).thenReturn("Read timed out");

        assertThat(GlobalExceptionHandler.resolveFeignClientMessage(ex))
                .containsIgnoringCase("timed out");
    }

    @Test
    void resolveFeignClientMessage_keepsProviderMessage() {
        FeignException ex = mock(FeignException.class);
        when(ex.status()).thenReturn(502);
        when(ex.contentUTF8())
                .thenReturn("{\"success\":false,\"error\":{\"message\":\"Failed to generate content\"}}");

        assertThat(GlobalExceptionHandler.resolveFeignClientMessage(ex))
                .isEqualTo("Failed to generate content");
    }
}
