package tn.esprit.gamification.Controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.gamification.Dto.AchievementProgressDTO;
import tn.esprit.gamification.Entities.UserAchievement;
import tn.esprit.gamification.Services.UserAchievementService;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserAchievementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserAchievementService service;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private org.springframework.security.web.SecurityFilterChain securityFilterChain;

    @Test
    void getUserAchievements_nominalCase() throws Exception {
        UserAchievement ua = new UserAchievement();
        ua.setId(10L);

        when(service.getUserAchievements(1L)).thenReturn(List.of(ua));

        mockMvc.perform(get("/api/user-achievements/{userId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10L));
    }

    @Test
    void getProgress_nominalCase() throws Exception {
        AchievementProgressDTO dto = AchievementProgressDTO.builder()
                .achievementId(10L)
                .title("Progress Test")
                .unlocked(false)
                .build();

        when(service.getUserProgress(1L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/user-achievements/{userId}/progress", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].achievementId").value(10L))
                .andExpect(jsonPath("$[0].title").value("Progress Test"))
                .andExpect(jsonPath("$[0].unlocked").value(false));
    }
}
