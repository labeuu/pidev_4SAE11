package com.esprit.task.controller;

import com.esprit.task.dto.TaskCommentRequest;
import com.esprit.task.entity.TaskComment;
import com.esprit.task.service.TaskCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/task-comments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Task Comments", description = "CRUD for task comments")
public class TaskCommentController {

    private final TaskCommentService taskCommentService;

    @GetMapping
    @Operation(summary = "List comments", description = "Returns paginated task comments.")
    @ApiResponse(responseCode = "200", description = "Success")
    public ResponseEntity<Page<TaskComment>> list(
            @Parameter(description = "Page index") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(taskCommentService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get comment by ID", description = "Returns a single comment by id.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "404", description = "Comment not found", content = @Content)
    })
    // Returns by id.
    public ResponseEntity<TaskComment> getById(@Parameter(description = "Comment ID", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(taskCommentService.findById(id));
    }

    @GetMapping("/task/{taskId}")
    @Operation(summary = "List by task", description = "Returns all comments for the given task.")
    @ApiResponse(responseCode = "200", description = "Success")
    // Returns by task id.
    public ResponseEntity<List<TaskComment>> getByTaskId(@Parameter(description = "Task ID", required = true) @PathVariable Long taskId) {
        return ResponseEntity.ok(taskCommentService.findByTaskId(taskId));
    }

    @PostMapping
    @Operation(summary = "Create comment", description = "Creates a new task comment.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created", content = @Content(schema = @Schema(implementation = TaskComment.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content)
    })
    // Creates this operation.
    public ResponseEntity<TaskComment> create(@RequestBody TaskCommentRequest request) {
        TaskComment comment = TaskComment.builder()
                .taskId(request.getTaskId())
                .userId(request.getUserId())
                .message(request.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(taskCommentService.create(comment));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update comment", description = "Updates an existing comment.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "404", description = "Comment not found", content = @Content)
    })
    public ResponseEntity<TaskComment> update(
            @Parameter(description = "Comment ID", required = true) @PathVariable Long id,
            @RequestBody TaskCommentRequest request) {
        TaskComment comment = TaskComment.builder()
                .taskId(request.getTaskId())
                .userId(request.getUserId())
                .message(request.getMessage())
                .build();
        return ResponseEntity.ok(taskCommentService.update(id, comment));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete comment", description = "Deletes a comment.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "No content"),
            @ApiResponse(responseCode = "404", description = "Comment not found", content = @Content)
    })
    // Deletes this operation.
    public ResponseEntity<Void> delete(@Parameter(description = "Comment ID", required = true) @PathVariable Long id) {
        taskCommentService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
