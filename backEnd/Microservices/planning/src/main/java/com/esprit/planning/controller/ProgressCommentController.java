package com.esprit.planning.controller;

import com.esprit.planning.dto.ProgressCommentPatchRequest;
import com.esprit.planning.dto.ProgressCommentRequest;
import com.esprit.planning.entity.ProgressComment;
import com.esprit.planning.service.ProgressCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for comments on progress updates: list (full or paged), get by id, by progress update, by user, create, update, patch, delete.
 * All endpoints are under /api/progress-comments.
 */
@RestController
@RequestMapping("/api/progress-comments")
@CrossOrigin(origins = "*")
@Tag(name = "Progress Comments", description = "Add and manage comments on progress updates")
public class ProgressCommentController {

    private final ProgressCommentService progressCommentService;

    public ProgressCommentController(ProgressCommentService progressCommentService) {
        this.progressCommentService = progressCommentService;
    }

    /** Returns progress comments with pagination (page, size, sort). Default: page=0, size=20, max size=100. */
    @GetMapping
    @Operation(
            summary = "List comments (paginated)",
            description = "Returns progress comments with pagination. Use page, size (max 100), and sort (e.g. createdAt,desc) query parameters."
    )
    @ApiResponse(responseCode = "200", description = "Success")
    public ResponseEntity<Page<ProgressComment>> getAll(
            @Parameter(description = "Page index (0-based)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)", example = "20") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort (e.g. createdAt,desc or createdAt,asc)") @RequestParam(required = false) String sort
    ) {
        int cappedSize = Math.min(Math.max(1, size), 100);
        Page<ProgressComment> paged = progressCommentService.findAllPaged(page, cappedSize, sort);
        return ResponseEntity.ok(paged);
    }

    /** Returns a single comment by its id. 404 if not found. */
    @GetMapping("/{id}")
    @Operation(summary = "Get comment by ID", description = "Returns a single comment by its id.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "404", description = "Comment not found", content = @Content)
    })
    public ResponseEntity<ProgressComment> getById(
            @Parameter(description = "Comment ID", example = "1", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(progressCommentService.findById(id));
    }

    /** Returns all comments for the given progress update. */
    @GetMapping("/progress-update/{progressUpdateId}")
    @Operation(summary = "List comments by progress update", description = "Returns all comments for a given progress update.")
    @ApiResponse(responseCode = "200", description = "Success")
    public ResponseEntity<List<ProgressComment>> getByProgressUpdateId(
            @Parameter(description = "Progress update ID", example = "1", required = true) @PathVariable Long progressUpdateId) {
        return ResponseEntity.ok(progressCommentService.findByProgressUpdateId(progressUpdateId));
    }

    /** Returns all comments created by the given user. */
    @GetMapping("/by-user/{userId}")
    @Operation(summary = "List comments by user", description = "Returns all comments created by the given user.")
    @ApiResponse(responseCode = "200", description = "Success")
    public ResponseEntity<List<ProgressComment>> getByUserId(
            @Parameter(description = "User ID", example = "5", required = true) @PathVariable Long userId) {
        return ResponseEntity.ok(progressCommentService.findByUserId(userId));
    }

    /** Creates a new comment on a progress update. Progress update must exist. Returns 201 with created comment. */
    @PostMapping
    @Operation(summary = "Create comment", description = "Adds a comment to a progress update. The progress update must exist (create one via Progress Updates first).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created", content = @Content(schema = @Schema(implementation = ProgressComment.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or progress update not found", content = @Content)
    })
    // Creates this operation.
    public ResponseEntity<ProgressComment> create(@RequestBody ProgressCommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(progressCommentService.create(
                        request.getProgressUpdateId(),
                        request.getUserId(),
                        request.getMessage()));
    }

    /** Updates the message of an existing comment. Returns 200 with updated comment; 404 if not found. */
    @PutMapping("/{id}")
    @Operation(summary = "Update comment", description = "Updates the message of an existing comment. Only message is updated; progressUpdateId and userId are ignored in the body.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success", content = @Content(schema = @Schema(implementation = ProgressComment.class))),
            @ApiResponse(responseCode = "404", description = "Comment not found", content = @Content)
    })
    public ResponseEntity<ProgressComment> update(
            @Parameter(description = "Comment ID", example = "1", required = true) @PathVariable Long id,
            @RequestBody ProgressCommentRequest request) {
        return ResponseEntity.ok(progressCommentService.update(id, request.getMessage()));
    }

    /** Partially updates a comment (currently only message). If no message in body, returns current comment unchanged. */
    @PatchMapping("/{id}")
    @Operation(
            summary = "Partially update comment",
            description = "Partially updates an existing comment. Currently only the message field is supported."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success", content = @Content(schema = @Schema(implementation = ProgressComment.class))),
            @ApiResponse(responseCode = "404", description = "Comment not found", content = @Content)
    })
    public ResponseEntity<ProgressComment> patch(
            @Parameter(description = "Comment ID", example = "1", required = true) @PathVariable Long id,
            @RequestBody ProgressCommentPatchRequest request
    ) {
        if (request.getMessage() != null) {
            return ResponseEntity.ok(progressCommentService.update(id, request.getMessage()));
        }
        return ResponseEntity.ok(progressCommentService.findById(id));
    }

    /** Deletes a comment by id. Returns 204 No Content. */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete comment", description = "Deletes a comment by id.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "No content"),
            @ApiResponse(responseCode = "404", description = "Comment not found", content = @Content)
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Comment ID", example = "1", required = true) @PathVariable Long id) {
        progressCommentService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
