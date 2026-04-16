package tn.esprit.gamification.Controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.gamification.Scheduler.FastResponderScheduler;
import tn.esprit.gamification.Scheduler.TopFreelancerScheduler;
import tn.esprit.gamification.Services.GamificationService;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GamificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class GamificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GamificationService service;

    @MockitoBean
    private TopFreelancerScheduler topFreelancerScheduler;

    @MockitoBean
    private FastResponderScheduler fastResponderScheduler;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private org.springframework.security.web.SecurityFilterChain securityFilterChain;

    @Test
    void projectCompleted_nominalCase() throws Exception {
        mockMvc.perform(post("/api/gamification/project-completed")
                .param("userId", "1"))
                .andExpect(status().isOk());

        verify(service).handleProjectCompleted(1L);
    }

    @Test
    void projectCreated_nominalCase() throws Exception {
        mockMvc.perform(post("/api/gamification/project-created")
                .param("userId", "1"))
                .andExpect(status().isOk());

        verify(service).handleProjectCreated(1L);
    }

    @Test
    void fastResponse_nominalCase() throws Exception {
        mockMvc.perform(post("/api/gamification/fast-response")
                .param("userId", "1"))
                .andExpect(status().isOk());

        verify(service).handleFastResponse(1L);
    }

    @Test
    void triggerTopFreelancer_nominalCase() throws Exception {
        mockMvc.perform(post("/api/gamification/trigger/top-freelancer"))
                .andExpect(status().isOk())
                .andExpect(content().string("✅ Top Freelancer détecté et récompensé"));

        verify(topFreelancerScheduler).detectAndRewardTopFreelancer();
    }

    @Test
    void triggerFastResponder_nominalCase() throws Exception {
        mockMvc.perform(post("/api/gamification/trigger/fast-responder"))
                .andExpect(status().isOk())
                .andExpect(content().string("✅ Fast Responders vérifiés"));

        verify(fastResponderScheduler).checkFastResponders();
    }

    @Test
    void recompute_nominalCase() throws Exception {
        mockMvc.perform(post("/api/gamification/recompute")
                .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string("✅ Catalogue réévalué pour l'utilisateur 1"));

        verify(service).recomputeAllAchievements(1L);
    }
}
