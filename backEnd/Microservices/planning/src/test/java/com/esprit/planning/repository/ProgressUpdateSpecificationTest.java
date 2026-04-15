package com.esprit.planning.repository;

import com.esprit.planning.entity.ProgressUpdate;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProgressUpdateSpecification. Verifies that the filtered specification
 * produces a non-null Predicate and that optional parameters are applied when present.
 */
class ProgressUpdateSpecificationTest {

    @Test
    void filtered_withAllEmpty_returnsPredicate() {
        Specification<ProgressUpdate> spec = ProgressUpdateSpecification.filtered(
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());

        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Root<ProgressUpdate> root = mock(Root.class);
        Predicate p = mock(Predicate.class);
        when(cb.and(any(Predicate[].class))).thenReturn(p);

        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isNotNull();
        verify(cb).and(any(Predicate[].class));
    }

    @Test
    void filtered_withProjectId_addsEqualPredicate() {
        @SuppressWarnings("unchecked")
        Path<Object> path = mock(Path.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Root<ProgressUpdate> root = mock(Root.class);
        when(root.get("projectId")).thenReturn(path);
        Predicate equalPred = mock(Predicate.class);
        when(cb.equal(path, 1L)).thenReturn(equalPred);
        when(cb.and(any(Predicate[].class))).thenReturn(mock(Predicate.class));

        Specification<ProgressUpdate> spec = ProgressUpdateSpecification.filtered(
                Optional.of(1L), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());

        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isNotNull();
        verify(root).get("projectId");
        verify(cb).equal(path, 1L);
    }

    @Test
    void filtered_withSearch_returnsNonNullPredicate() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Root<ProgressUpdate> root = mock(Root.class);
        @SuppressWarnings("unchecked")
        Path<Object> path = mock(Path.class);
        @SuppressWarnings("unchecked")
        Expression<String> lowerExpr = mock(Expression.class);
        when(root.get("title")).thenReturn(path);
        when(root.get("description")).thenReturn(path);
        when(cb.lower(any())).thenReturn(lowerExpr);
        when(cb.like(any(Expression.class), anyString())).thenReturn(mock(Predicate.class));
        when(cb.isNotNull(any())).thenReturn(mock(Predicate.class));
        when(cb.or(any(), any())).thenReturn(mock(Predicate.class));
        when(cb.and(any(Predicate[].class))).thenReturn(mock(Predicate.class));

        Specification<ProgressUpdate> spec = ProgressUpdateSpecification.filtered(
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of("test"), Optional.empty());

        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isNotNull();
    }
}
