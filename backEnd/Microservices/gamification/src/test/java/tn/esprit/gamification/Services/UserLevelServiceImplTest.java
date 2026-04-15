package tn.esprit.gamification.Services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import tn.esprit.gamification.Dto.LeaderboardEntryDTO;
import tn.esprit.gamification.Dto.UserLevelSummaryDTO;
import tn.esprit.gamification.Entities.UserLevel;
import tn.esprit.gamification.Repository.UserLevelRepository;
import tn.esprit.gamification.client.UserClient;
import tn.esprit.gamification.client.UserClient.UserResponseDTO;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserLevelServiceImplTest {

    @Mock
    private UserLevelRepository repository;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private UserLevelServiceImpl service;

    @Test
    void getUserLevel_existing_returnsLevel() {
        UserLevel ul = new UserLevel();
        ul.setUserId(1L);
        ul.setUserRole("FREELANCER");
        when(repository.findByUserId(1L)).thenReturn(Optional.of(ul));

        UserLevel result = service.getUserLevel(1L);

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(1L);
        verify(userClient, never()).getUserById(any());
    }

    @Test
    void getUserLevel_notFound_createsNewAndFetchesRole() {
        when(repository.findByUserId(1L)).thenReturn(Optional.empty());
        UserResponseDTO user = new UserResponseDTO();
        user.setRole("FREELANCER");
        when(userClient.getUserById(1L)).thenReturn(user);

        UserLevel saved = new UserLevel();
        saved.setUserId(1L);
        saved.setUserRole("FREELANCER");
        when(repository.save(any(UserLevel.class))).thenReturn(saved);

        UserLevel result = service.getUserLevel(1L);

        assertThat(result).isNotNull();
        assertThat(result.getUserRole()).isEqualTo("FREELANCER");
        verify(userClient).getUserById(1L);
        verify(repository).save(any(UserLevel.class));
    }

    @Test
    void addXp_nominalCase_updatesXpAndLevel() throws Exception {
        // Inject xpScaleFactor
        Field f = UserLevelServiceImpl.class.getDeclaredField("xpScaleFactor");
        f.setAccessible(true);
        f.set(service, 50);

        UserLevel ul = new UserLevel();
        ul.setUserId(1L);
        ul.setUserRole("FREELANCER");
        ul.setXp(10);
        when(repository.findByUserId(1L)).thenReturn(Optional.of(ul));

        service.addXp(1L, 90); // Total 100 XP -> level = floor(sqrt(100/50)) + 1 = sqrt(2) + 1 = 1 + 1 = 2

        verify(repository).updateXpAndLevel(1L, 100, 2);
    }

    @Test
    void getLeaderboard_nominalCase() {
        UserLevel ul = new UserLevel();
        ul.setUserId(1L);
        ul.setXp(500);
        ul.setLevel(4);
        
        when(repository.findLeaderboard(any(PageRequest.class))).thenReturn(List.of(ul));
        
        UserResponseDTO user = new UserResponseDTO();
        user.setFirstName("John");
        user.setLastName("Doe");
        when(userClient.getUserById(1L)).thenReturn(user);

        List<LeaderboardEntryDTO> result = service.getLeaderboard(10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFullName()).isEqualTo("John Doe");
        assertThat(result.get(0).getXp()).isEqualTo(500);
    }
}
