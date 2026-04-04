package com.esprit.task.repository;

import com.esprit.task.entity.Subtask;
import com.esprit.task.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SubtaskRepository extends JpaRepository<Subtask, Long> {

    List<Subtask> findByParent_IdOrderByOrderIndexAsc(Long parentTaskId);

    void deleteByParent_Id(Long parentTaskId);

    @Query("SELECT COALESCE(MAX(s.orderIndex), 0) FROM Subtask s WHERE s.parent.id = :parentId")
    Integer findMaxOrderIndexByParent(@Param("parentId") Long parentId);

    List<Subtask> findByProjectIdOrderByParent_IdAscOrderIndexAsc(Long projectId);

    @Query("SELECT s FROM Subtask s WHERE s.dueDate IS NOT NULL AND s.dueDate < :today AND s.status NOT IN ('DONE', 'CANCELLED') ORDER BY s.projectId, s.orderIndex, s.createdAt DESC")
    List<Subtask> findOverdueSubtasks(@Param("today") LocalDate today);

    @Query("SELECT s FROM Subtask s WHERE s.projectId = :projectId AND s.dueDate IS NOT NULL AND s.dueDate < :today AND s.status NOT IN ('DONE', 'CANCELLED') ORDER BY s.orderIndex, s.createdAt DESC")
    List<Subtask> findOverdueSubtasksByProject(@Param("projectId") Long projectId, @Param("today") LocalDate today);

    @Query("SELECT s FROM Subtask s WHERE s.assigneeId = :assigneeId AND s.dueDate IS NOT NULL AND s.dueDate < :today AND s.status NOT IN ('DONE', 'CANCELLED') ORDER BY s.projectId, s.orderIndex, s.createdAt DESC")
    List<Subtask> findOverdueSubtasksByAssignee(@Param("assigneeId") Long assigneeId, @Param("today") LocalDate today);

    @Query("SELECT s FROM Subtask s WHERE s.dueDate IS NOT NULL AND s.dueDate >= :start AND s.dueDate <= :end ORDER BY s.projectId, s.orderIndex, s.createdAt DESC")
    List<Subtask> findByDueDateBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT s FROM Subtask s WHERE s.dueDate IS NOT NULL AND s.dueDate >= :start AND s.dueDate <= :end AND s.assigneeId = :userId ORDER BY s.projectId, s.orderIndex, s.createdAt DESC")
    List<Subtask> findByDueDateBetweenAndAssigneeId(
            @Param("start") LocalDate start, @Param("end") LocalDate end, @Param("userId") Long userId);

    long countByProjectId(Long projectId);

    long countByProjectIdAndStatus(Long projectId, TaskStatus status);

    List<Subtask> findByAssigneeId(Long assigneeId);

    @Query("SELECT s FROM Subtask s WHERE s.dueDate IS NOT NULL AND s.dueDate >= :from AND s.dueDate <= :to AND s.status NOT IN ('DONE', 'CANCELLED')")
    List<Subtask> findDueSoonSubtasks(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT s FROM Subtask s WHERE s.dueDate IS NOT NULL AND s.dueDate >= :from AND s.dueDate <= :to AND s.status NOT IN ('DONE', 'CANCELLED') AND s.projectId = :projectId")
    List<Subtask> findDueSoonSubtasksByProject(@Param("from") LocalDate from, @Param("to") LocalDate to, @Param("projectId") Long projectId);

    @Query("SELECT s FROM Subtask s WHERE s.dueDate IS NOT NULL AND s.dueDate >= :from AND s.dueDate <= :to AND s.status NOT IN ('DONE', 'CANCELLED') AND s.assigneeId = :assigneeId")
    List<Subtask> findDueSoonSubtasksByAssignee(@Param("from") LocalDate from, @Param("to") LocalDate to, @Param("assigneeId") Long assigneeId);

    @Query("SELECT s.parent.id, COUNT(s), COALESCE(SUM(CASE WHEN s.status = com.esprit.task.entity.TaskStatus.DONE THEN 1 ELSE 0 END), 0) "
            + "FROM Subtask s WHERE s.parent.id IN :taskIds AND s.parent.assigneeId = :assigneeId "
            + "AND s.status <> com.esprit.task.entity.TaskStatus.CANCELLED GROUP BY s.parent.id")
    List<Object[]> aggregateSubtaskProgressByParent(
            @Param("assigneeId") Long assigneeId, @Param("taskIds") List<Long> taskIds);

    @Query("SELECT t.projectId, MAX(s.updatedAt) FROM Subtask s JOIN s.parent t WHERE t.assigneeId = :assigneeId GROUP BY t.projectId")
    List<Object[]> findMaxSubtaskUpdatedByProjectForAssignee(@Param("assigneeId") Long assigneeId);
}
