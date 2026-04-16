package tn.esprit.gamification.Controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.gamification.Dto.LeaderboardEntryDTO;
import tn.esprit.gamification.Dto.UserLevelSummaryDTO;
import tn.esprit.gamification.Entities.UserLevel;
import tn.esprit.gamification.Services.UserLevelService;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserLevelController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserLevelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserLevelService service;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private org.springframework.security.web.SecurityFilterChain securityFilterChain;

    @Test
    void getLevel_nominalCase() throws Exception {
        UserLevel ul = new UserLevel();
        ul.setUserId(1L);
        ul.setXp(150);
        ul.setLevel(2);

        when(service.getUserLevel(1L)).thenReturn(ul);

        mockMvc.perform(get("/api/user-level/{userId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.xp").value(150))
                .andExpect(jsonPath("$.level").value(2));
    }

    @Test
    void getLevelSummary_nominalCase() throws Exception {
        UserLevelSummaryDTO summary = UserLevelSummaryDTO.builder()
                .userId(1L)
                .xp(150)
                .level(2)
                .activeStreak(5)
                .build();

        when(service.getUserLevelSummary(1L)).thenReturn(summary);

        mockMvc.perform(get("/api/user-level/{userId}/summary", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.xp").value(150))
                .andExpect(jsonPath("$.level").value(2))
                .andExpect(jsonPath("$.activeStreak").value(5));
    }

    @Test
    void getLeaderboard_nominalCase() throws Exception {
        LeaderboardEntryDTO entry = LeaderboardEntryDTO.builder()
                .rank(1)
                .userId(1L)
                .fullName("John Doe")
                .xp(500)
                .level(3)
                .build();

        when(service.getLeaderboard(10)).thenReturn(List.of(entry));

        mockMvc.perform(get("/api/user-level/leaderboard")
                .param("top", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rank").value(1))
                .andExpect(jsonPath("$[0].userId").value(1L))
                .andExpect(jsonPath("$[0].fullName").value("John Doe"))
                .andExpect(jsonPath("$[0].xp").value(500))
                .andExpect(jsonPath("$[0].level").value(3));
    }

    @Test
    void updateStreak_nominalCase() throws Exception {
        when(service.updateAndGetActiveStreak(1L)).thenReturn(5);

        mockMvc.perform(post("/api/user-level/{userId}/streak/update", 1L))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }
}
