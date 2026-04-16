package tn.esprit.gamification.Controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.gamification.Dto.RecommendationDTO;
import tn.esprit.gamification.Services.RecommendationService;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecommendationController.class)
@AutoConfigureMockMvc(addFilters = false)
class RecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecommendationService service;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private org.springframework.security.web.SecurityFilterChain securityFilterChain;

    @Test
    void getRecommendations_nominalCase() throws Exception {
        RecommendationDTO rec = new RecommendationDTO("Keep pushing!", 1);

        when(service.getRecommendations(1L)).thenReturn(List.of(rec));

        mockMvc.perform(get("/api/recommendations/{userId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].message").value("Keep pushing!"))
                .andExpect(jsonPath("$[0].priority").value(1));
    }
}
