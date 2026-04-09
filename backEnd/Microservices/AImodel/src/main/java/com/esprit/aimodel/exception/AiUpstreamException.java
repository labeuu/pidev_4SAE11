package com.esprit.aimodel.exception;

import org.springframework.http.HttpStatus;

public class AiUpstreamException extends RuntimeException {

    private final HttpStatus status;

    public AiUpstreamException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
