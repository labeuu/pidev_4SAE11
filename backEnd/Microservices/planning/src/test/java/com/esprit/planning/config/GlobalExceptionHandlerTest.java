package com.esprit.planning.config;

import com.esprit.planning.exception.EntityNotFoundException;
import com.esprit.planning.exception.ProgressCannotDecreaseException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for GlobalExceptionHandler. Verifies EntityNotFoundException maps to 404.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleEntityNotFound_returns404WithErrorEnvelope() {
        EntityNotFoundException ex = new EntityNotFoundException("ProgressUpdate", 999L);

        ResponseEntity<Map<String, Object>> result = handler.handleEntityNotFound(ex);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(result.getBody()).containsKey("error");
        @SuppressWarnings("unchecked")
        Map<String, Object> error = (Map<String, Object>) result.getBody().get("error");
        assertThat(error).containsEntry("code", "NOT_FOUND");
        assertThat(error.get("message")).asString().contains("ProgressUpdate not found");
        assertThat(error.get("message")).asString().contains("999");
        assertThat(error.get("details")).isEqualTo(List.of());
    }

    @Test
    void handleProgressCannotDecrease_returns400WithMinAndProvided() {
        ProgressCannotDecreaseException ex = new ProgressCannotDecreaseException(50, 10);

        ResponseEntity<Map<String, Object>> result = handler.handleProgressCannotDecrease(ex);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        @SuppressWarnings("unchecked")
        Map<String, Object> error = (Map<String, Object>) result.getBody().get("error");
        assertThat(error).containsEntry("code", "VALIDATION_ERROR");
        assertThat(error).containsEntry("minAllowed", 50);
        assertThat(error).containsEntry("provided", 10);
        assertThat(error.get("message")).asString().contains("50%");
    }

    @Test
    void handleValidation_returns400WithFieldDetails() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("progressUpdateRequest", "title", "must not be blank")));

        ResponseEntity<Map<String, Object>> result = handler.handleValidation(ex);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        @SuppressWarnings("unchecked")
        Map<String, Object> error = (Map<String, Object>) result.getBody().get("error");
        assertThat(error).containsEntry("code", "VALIDATION_ERROR");
        @SuppressWarnings("unchecked")
        List<String> details = (List<String>) error.get("details");
        assertThat(details).anyMatch(s -> s.contains("title") && s.contains("must not be blank"));
    }

    @Test
    void handleValidation_mapsMultipleFieldErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("req", "a", "err a"),
                new FieldError("req", "b", "err b")));

        ResponseEntity<Map<String, Object>> result = handler.handleValidation(ex);

        @SuppressWarnings("unchecked")
        List<String> details = (List<String>) ((Map<String, Object>) result.getBody().get("error")).get("details");
        assertThat(details).containsExactly("a: err a", "b: err b");
    }

    @Test
    void handleEntityNotFound_withMessageOnlyConstructor_mapsTo404() {
        EntityNotFoundException ex = new EntityNotFoundException("Resource missing");

        ResponseEntity<Map<String, Object>> result = handler.handleEntityNotFound(ex);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        @SuppressWarnings("unchecked")
        Map<String, Object> error = (Map<String, Object>) result.getBody().get("error");
        assertThat(error.get("message")).isEqualTo("Resource missing");
    }

    @Test
    void handleIllegalArgument_returns400() {
        ResponseEntity<Map<String, Object>> result = handler.handleIllegalArgument(new IllegalArgumentException("bad id"));

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        @SuppressWarnings("unchecked")
        Map<String, Object> error = (Map<String, Object>) result.getBody().get("error");
        assertThat(error).containsEntry("code", "BAD_REQUEST");
        assertThat(error.get("message")).isEqualTo("bad id");
    }
}
