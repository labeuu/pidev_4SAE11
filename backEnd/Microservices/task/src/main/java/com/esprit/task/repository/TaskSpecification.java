package com.esprit.task.repository;

import com.esprit.task.entity.Task;
import com.esprit.task.entity.TaskPriority;
import com.esprit.task.entity.TaskStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public final class TaskSpecification {

    private TaskSpecification() {
    }

    /**
     * Open tasks with a due date between {@code from} and {@code to} (inclusive on both dates).
     */
    public static Specification<Task> dueSoon(
            LocalDate from,
            LocalDate to,
            Optional<Long> projectId,
            Optional<Long> assigneeId,
            Optional<Collection<Long>> allowedProjectIds) {
        return (Root<Task> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNotNull(root.get("dueDate")));
            predicates.add(cb.greaterThanOrEqualTo(root.get("dueDate"), from));
            predicates.add(cb.lessThanOrEqualTo(root.get("dueDate"), to));
            predicates.add(cb.not(cb.or(
                    cb.equal(root.get("status"), TaskStatus.DONE),
                    cb.equal(root.get("status"), TaskStatus.CANCELLED))));
            projectId.ifPresent(id -> predicates.add(cb.equal(root.get("projectId"), id)));
            assigneeId.ifPresent(id -> predicates.add(cb.equal(root.get("assigneeId"), id)));
            allowedProjectIds.ifPresent(ids -> predicates.add(root.get("projectId").in(ids)));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Task> filtered(
            Optional<Long> projectId,
            Optional<Long> contractId,
            Optional<Long> assigneeId,
            Optional<TaskStatus> status,
            Optional<TaskPriority> priority,
            Optional<String> search,
            Optional<LocalDate> dueDateFrom,
            Optional<LocalDate> dueDateTo,
            Optional<Boolean> openTasksOnly,
            Optional<Collection<Long>> allowedProjectIds) {
        return (Root<Task> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            projectId.ifPresent(id -> predicates.add(cb.equal(root.get("projectId"), id)));
            allowedProjectIds.ifPresent(ids -> predicates.add(root.get("projectId").in(ids)));
            contractId.ifPresent(id -> predicates.add(cb.equal(root.get("contractId"), id)));
            assigneeId.ifPresent(id -> predicates.add(cb.equal(root.get("assigneeId"), id)));
            status.ifPresent(s -> predicates.add(cb.equal(root.get("status"), s)));
            priority.ifPresent(p -> predicates.add(cb.equal(root.get("priority"), p)));

            if (openTasksOnly.map(Boolean::booleanValue).orElse(false) && status.isEmpty()) {
                predicates.add(cb.not(root.get("status").in(TaskStatus.DONE, TaskStatus.CANCELLED)));
            }

            dueDateFrom.ifPresent(from -> predicates.add(cb.greaterThanOrEqualTo(root.get("dueDate"), from)));
            dueDateTo.ifPresent(to -> predicates.add(cb.lessThanOrEqualTo(root.get("dueDate"), to)));

            search.filter(s -> s != null && !s.isBlank()).ifPresent(keyword -> {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate titleLike = cb.like(cb.lower(root.get("title")), pattern);
                Predicate descLike = cb.and(
                        cb.isNotNull(root.get("description")),
                        cb.like(cb.lower(root.get("description")), pattern));
                predicates.add(cb.or(titleLike, descLike));
            });

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
