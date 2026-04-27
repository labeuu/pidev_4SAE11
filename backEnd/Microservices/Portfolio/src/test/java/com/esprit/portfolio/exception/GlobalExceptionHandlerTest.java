package com.esprit.portfolio.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleRuntimeReturns500WithMessage() {
        ResponseEntity<Map<String, Object>> res =
                handler.handleRuntimeException(new RuntimeException("svc-down"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, res.getStatusCode());
        assertEquals("svc-down", res.getBody().get("message"));
    }

    @Test
    void handleIllegalArgumentReturns400() {
        ResponseEntity<Map<String, Object>> res =
                handler.handleIllegalArgumentException(new IllegalArgumentException("bad"));
        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
        assertEquals("bad", res.getBody().get("message"));
    }

    @Test
    void handleTypeMismatchEmbedsAllowedDomains() {
        var ex = new MethodArgumentTypeMismatchException(null, null, "domain", null,
                new IllegalArgumentException("bad"));
        ResponseEntity<Map<String, Object>> res = handler.handleTypeMismatch(ex);
        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
        assertTrue(res.getBody().get("message").toString().contains("Allowed values"));
    }

    @Test
    void handleGenericExceptionReturns500() throws Exception {
        ResponseEntity<Map<String, Object>> res =
                handler.handleGenericException(new Exception("oops"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, res.getStatusCode());
        assertTrue(res.getBody().get("message").toString().contains("oops"));
    }
}
