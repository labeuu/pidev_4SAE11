package com.esprit.planning.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EntityNotFoundExceptionTest {

    @Test
    void messageConstructor_setsMessage() {
        EntityNotFoundException ex = new EntityNotFoundException("custom message");
        assertThat(ex.getMessage()).isEqualTo("custom message");
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    void entityAndIdConstructor_formatsMessage() {
        EntityNotFoundException ex = new EntityNotFoundException("ProgressUpdate", 42L);
        assertThat(ex.getMessage()).isEqualTo("ProgressUpdate not found with id: 42");
    }

    @Test
    void inheritsRuntimeExceptionStackTrace() {
        EntityNotFoundException ex = new EntityNotFoundException("gone");
        assertThat(ex.getStackTrace()).isNotEmpty();
        assertThat(ex.fillInStackTrace()).isSameAs(ex);
    }
}
