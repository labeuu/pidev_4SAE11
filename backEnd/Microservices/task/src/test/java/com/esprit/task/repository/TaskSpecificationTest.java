package com.esprit.task.repository;

import com.esprit.task.entity.Task;
import com.esprit.task.entity.TaskPriority;
import com.esprit.task.entity.TaskStatus;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for TaskSpecification. Verifies that the specification can be built.
 */
class TaskSpecificationTest {

    @Test
    void filtered_withProjectId_returnsNonNullSpec() {
        Specification<Task> spec = TaskSpecification.filtered(
                Optional.of(1L), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty());

        assertThat(spec).isNotNull();
    }

    @Test
    void filtered_withSearch_returnsNonNullSpec() {
        Specification<Task> spec = TaskSpecification.filtered(
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.of("test"),
                Optional.empty(), Optional.empty());

        assertThat(spec).isNotNull();
    }

    @Test
    void filtered_emptyOptional_returnsNonNullSpec() {
        Specification<Task> spec = TaskSpecification.filtered(
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty());

        assertThat(spec).isNotNull();
    }

    @Test
    void filtered_withContractId_returnsNonNullSpec() {
        Specification<Task> spec = TaskSpecification.filtered(
                Optional.empty(), Optional.of(2L), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty());
        assertThat(spec).isNotNull();
    }

    @Test
    void filtered_withAssigneeId_returnsNonNullSpec() {
        Specification<Task> spec = TaskSpecification.filtered(
                Optional.empty(), Optional.empty(), Optional.of(10L),
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty());
        assertThat(spec).isNotNull();
    }

    @Test
    void filtered_withStatus_returnsNonNullSpec() {
        Specification<Task> spec = TaskSpecification.filtered(
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.of(TaskStatus.IN_PROGRESS), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty());
        assertThat(spec).isNotNull();
    }

    @Test
    void filtered_withPriority_returnsNonNullSpec() {
        Specification<Task> spec = TaskSpecification.filtered(
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.of(TaskPriority.HIGH), Optional.empty(),
                Optional.empty(), Optional.empty());
        assertThat(spec).isNotNull();
    }

    @Test
    void filtered_withDueDateFromOnly_returnsNonNullSpec() {
        Specification<Task> spec = TaskSpecification.filtered(
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.of(LocalDate.of(2024, 1, 1)),
                Optional.empty());
        assertThat(spec).isNotNull();
    }

    @Test
    void filtered_withDueDateRange_returnsNonNullSpec() {
        Specification<Task> spec = TaskSpecification.filtered(
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.of(LocalDate.of(2024, 1, 1)),
                Optional.of(LocalDate.of(2024, 12, 31)));
        assertThat(spec).isNotNull();
    }

    @Test
    void dueSoon_withProjectAndAssignee_returnsNonNullSpec() {
        Specification<Task> spec = TaskSpecification.dueSoon(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                Optional.of(1L),
                Optional.of(10L));
        assertThat(spec).isNotNull();
    }
}
