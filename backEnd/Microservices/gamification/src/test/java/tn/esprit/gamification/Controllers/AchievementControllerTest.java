package tn.esprit.gamification.Controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.gamification.Dto.AchievementDTO;
import tn.esprit.gamification.Entities.Achievement;
import tn.esprit.gamification.Mapper.AchievementMapper;
import tn.esprit.gamification.Services.AchievementService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AchievementController.class)
@AutoConfigureMockMvc(addFilters = false) // Bypass Security
class AchievementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AchievementService service;

    @MockitoBean
    private AchievementMapper mapper;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private org.springframework.security.web.SecurityFilterChain securityFilterChain;

    @Test
    void create_nominalCase() throws Exception {
        AchievementDTO dto = new AchievementDTO();
        dto.setTitle("Test Achievement");

        Achievement entity = new Achievement();
        entity.setTitle("Test Achievement");

        Achievement saved = new Achievement();
        saved.setId(10L);
        saved.setTitle("Test Achievement");

        when(mapper.toEntity(any(AchievementDTO.class))).thenReturn(entity);
        when(service.create(entity)).thenReturn(saved);

        mockMvc.perform(post("/api/achievements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L));
    }

    @Test
    void getAll_nominalCase() throws Exception {
        Achievement saved = new Achievement();
        saved.setId(10L);

        when(service.getAll()).thenReturn(List.of(saved));

        mockMvc.perform(get("/api/achievements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(10L));
    }

    @Test
    void delete_nominalCase() throws Exception {
        mockMvc.perform(delete("/api/achievements/{id}", 10L))
                .andExpect(status().isOk());

        verify(service).delete(10L);
    }

    @Test
    void update_nominalCase() throws Exception {
        AchievementDTO dto = new AchievementDTO();
        dto.setTitle("Updated");

        Achievement entity = new Achievement();
        entity.setTitle("Updated");

        Achievement saved = new Achievement();
        saved.setId(10L);
        saved.setTitle("Updated");

        when(mapper.toEntity(any(AchievementDTO.class))).thenReturn(entity);
        when(service.update(eq(10L), any(Achievement.class))).thenReturn(saved);

        mockMvc.perform(put("/api/achievements/{id}", 10L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.title").value("Updated"));
    }
}
