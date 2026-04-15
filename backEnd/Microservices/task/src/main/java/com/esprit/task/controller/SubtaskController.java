package com.esprit.task.controller;

import com.esprit.task.dto.SubtaskRequest;
import com.esprit.task.dto.SubtaskResponse;
import com.esprit.task.entity.TaskStatus;
import com.esprit.task.service.SubtaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/subtasks")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Subtasks", description = "CRUD for subtasks linked to a root task")
public class SubtaskController {

    private final SubtaskService subtaskService;

    @PutMapping("/{id}")
    @Operation(summary = "Update subtask")
    public ResponseEntity<SubtaskResponse> update(
            @Parameter(description = "Subtask ID", required = true) @PathVariable Long id,
            @Valid @RequestBody SubtaskRequest request) {
        return ResponseEntity.ok(subtaskService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change subtask status")
    public ResponseEntity<SubtaskResponse> patchStatus(
            @PathVariable Long id,
            @RequestParam TaskStatus status) {
        return ResponseEntity.ok(subtaskService.patchStatus(id, status));
    }

    @PatchMapping("/{id}/due-date")
    @Operation(summary = "Set or clear subtask due date")
    public ResponseEntity<SubtaskResponse> patchDueDate(
            @PathVariable Long id,
            @RequestParam(required = false) LocalDate dueDate) {
        return ResponseEntity.ok(subtaskService.patchDueDate(id, dueDate));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete subtask")
    // Deletes this operation.
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        subtaskService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
