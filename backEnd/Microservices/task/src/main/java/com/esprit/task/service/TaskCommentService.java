package com.esprit.task.service;

import com.esprit.task.entity.TaskComment;
import com.esprit.task.exception.EntityNotFoundException;
import com.esprit.task.repository.TaskCommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskCommentService {

    private final TaskCommentRepository taskCommentRepository;

    @Transactional(readOnly = true)
    // Finds all.
    public Page<TaskComment> findAll(Pageable pageable) {
        return taskCommentRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    // Finds by id.
    public TaskComment findById(Long id) {
        return taskCommentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("TaskComment", id));
    }

    @Transactional(readOnly = true)
    // Finds by task id.
    public List<TaskComment> findByTaskId(Long taskId) {
        return taskCommentRepository.findByTaskIdOrderByCreatedAtAsc(taskId);
    }

    @Transactional
    // Creates this operation.
    public TaskComment create(TaskComment comment) {
        return taskCommentRepository.save(comment);
    }

    @Transactional
    // Updates this operation.
    public TaskComment update(Long id, TaskComment comment) {
        TaskComment existing = findById(id);
        existing.setMessage(comment.getMessage());
        return taskCommentRepository.save(existing);
    }

    @Transactional
    // Deletes by id.
    public void deleteById(Long id) {
        TaskComment comment = findById(id);
        taskCommentRepository.delete(comment);
    }
}
