package tn.esprit.gamification.Services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.gamification.Entities.Achievement;
import tn.esprit.gamification.Entities.Enums.conditionType;
import tn.esprit.gamification.Repository.AchievementRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AchievementServiceImplTest {

    @Mock
    private AchievementRepository repository;

    @InjectMocks
    private AchievementServiceImpl service;

    @Test
    void create_nominalCase() {
        Achievement a = new Achievement();
        a.setTitle("Test");
        
        when(repository.save(any(Achievement.class))).thenReturn(a);

        Achievement result = service.create(a);

        assertThat(result.getTitle()).isEqualTo("Test");
        verify(repository).save(a);
    }

    @Test
    void getAll_nominalCase() {
        Achievement a1 = new Achievement();
        Achievement a2 = new Achievement();
        when(repository.findAll()).thenReturn(List.of(a1, a2));

        List<Achievement> result = service.getAll();

        assertThat(result).hasSize(2);
        verify(repository).findAll();
    }

    @Test
    void getById_exists_returnsAchievement() {
        Achievement a = new Achievement();
        a.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(a));

        Achievement result = service.getById(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getById_notFound_returnsNull() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        Achievement result = service.getById(1L);

        assertThat(result).isNull();
    }

    @Test
    void delete_nominalCase() {
        service.delete(1L);
        verify(repository).deleteById(1L);
    }

    @Test
    void update_exists_updatesAndReturns() {
        Achievement existing = new Achievement();
        existing.setId(1L);
        existing.setTitle("Old");

        Achievement updatePayload = new Achievement();
        updatePayload.setTitle("New");

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any(Achievement.class))).thenReturn(existing);

        Achievement result = service.update(1L, updatePayload);

        assertThat(result).isNotNull();
        assertThat(existing.getTitle()).isEqualTo("New");
        verify(repository).save(existing);
    }

    @Test
    void update_notFound_returnsNull() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        Achievement result = service.update(1L, new Achievement());

        assertThat(result).isNull();
        verify(repository, never()).save(any());
    }

    @Test
    void getByType_nominalCase() {
        Achievement a = new Achievement();
        when(repository.findByConditionType(conditionType.PROJECT_COMPLETED)).thenReturn(List.of(a));

        List<Achievement> result = service.getByType(conditionType.PROJECT_COMPLETED);

        assertThat(result).hasSize(1);
    }
}
