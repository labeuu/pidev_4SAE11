package com.esprit.planning.controller;

import com.esprit.planning.entity.ProgressComment;
import com.esprit.planning.entity.ProgressUpdate;
import com.esprit.planning.service.ProgressCommentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for ProgressCommentController. Verifies CRUD and list endpoints for progress comments.
 */
@WebMvcTest(ProgressCommentController.class)
class ProgressCommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProgressCommentService progressCommentService;

    @Test
    void getAll_withDefaults_returnsPagedResult() throws Exception {
        ProgressComment c = comment(1L, 1L, 5L, "Hello");
        Page<ProgressComment> page = new PageImpl<>(List.of(c), PageRequest.of(0, 20), 1);
        when(progressCommentService.findAllPaged(0, 20, null)).thenReturn(page);

        mockMvc.perform(get("/api/progress-comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].message").value("Hello"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getAll_withPageAndSize_returnsPagedResult() throws Exception {
        ProgressComment c = comment(1L, 1L, 5L, "Hello");
        Page<ProgressComment> page = new PageImpl<>(List.of(c), PageRequest.of(0, 20), 1);
        when(progressCommentService.findAllPaged(0, 20, null)).thenReturn(page);

        mockMvc.perform(get("/api/progress-comments").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getAll_withSizeAbove100_capsAt100() throws Exception {
        when(progressCommentService.findAllPaged(0, 100, null)).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/progress-comments").param("size", "500"))
                .andExpect(status().isOk());

        verify(progressCommentService).findAllPaged(0, 100, null);
    }

    @Test
    void getAll_withSort_passesSortToService() throws Exception {
        when(progressCommentService.findAllPaged(0, 20, "createdAt,desc")).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/progress-comments").param("sort", "createdAt,desc"))
                .andExpect(status().isOk());

        verify(progressCommentService).findAllPaged(0, 20, "createdAt,desc");
    }

    @Test
    void getById_returnsComment() throws Exception {
        ProgressComment c = comment(1L, 1L, 5L, "Nice work");
        when(progressCommentService.findById(1L)).thenReturn(c);

        mockMvc.perform(get("/api/progress-comments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.message").value("Nice work"));
    }

    @Test
    void getByProgressUpdateId_returnsList() throws Exception {
        when(progressCommentService.findByProgressUpdateId(10L)).thenReturn(List.of(comment(1L, 10L, 5L, "Comment")));

        mockMvc.perform(get("/api/progress-comments/progress-update/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].message").value("Comment"));
    }

    @Test
    void getByUserId_returnsList() throws Exception {
        when(progressCommentService.findByUserId(5L)).thenReturn(List.of(comment(1L, 1L, 5L, "My comment")));

        mockMvc.perform(get("/api/progress-comments/by-user/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(5));
    }

    @Test
    void create_returns201AndBody() throws Exception {
        ProgressComment created = comment(1L, 1L, 5L, "New comment");
        when(progressCommentService.create(1L, 5L, "New comment")).thenReturn(created);

        mockMvc.perform(post("/api/progress-comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"progressUpdateId\":1,\"userId\":5,\"message\":\"New comment\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.message").value("New comment"));
        verify(progressCommentService).create(1L, 5L, "New comment");
    }

    @Test
    void update_returns200AndBody() throws Exception {
        ProgressComment updated = comment(1L, 1L, 5L, "Updated message");
        when(progressCommentService.update(1L, "Updated message")).thenReturn(updated);

        mockMvc.perform(put("/api/progress-comments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"progressUpdateId\":1,\"userId\":5,\"message\":\"Updated message\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Updated message"));
        verify(progressCommentService).update(1L, "Updated message");
    }

    @Test
    void patch_withMessage_returns200AndUpdatedComment() throws Exception {
        ProgressComment updated = comment(1L, 1L, 5L, "Patched message");
        when(progressCommentService.update(1L, "Patched message")).thenReturn(updated);

        mockMvc.perform(patch("/api/progress-comments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Patched message\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Patched message"));
    }

    @Test
    void patch_withoutMessage_returnsCurrentComment() throws Exception {
        ProgressComment current = comment(1L, 1L, 5L, "Unchanged");
        when(progressCommentService.findById(1L)).thenReturn(current);

        mockMvc.perform(patch("/api/progress-comments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Unchanged"));
        verify(progressCommentService).findById(1L);
    }

    @Test
    void patch_withExplicitNullMessage_returnsCurrentComment() throws Exception {
        ProgressComment current = comment(1L, 1L, 5L, "Same");
        when(progressCommentService.findById(1L)).thenReturn(current);

        mockMvc.perform(patch("/api/progress-comments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Same"));
        verify(progressCommentService).findById(1L);
        verify(progressCommentService, never()).update(anyLong(), any());
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/progress-comments/1"))
                .andExpect(status().isNoContent());
        verify(progressCommentService).deleteById(1L);
    }

    private static ProgressComment comment(Long id, Long progressUpdateId, Long userId, String message) {
        ProgressUpdate pu = new ProgressUpdate();
        pu.setId(progressUpdateId);
        ProgressComment c = ProgressComment.builder()
                .id(id)
                .progressUpdate(pu)
                .userId(userId)
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();
        return c;
    }
}
