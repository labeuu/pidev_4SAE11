package tn.esprit.gamification.Evaluator;

import org.junit.jupiter.api.Test;
import tn.esprit.gamification.Entities.Enums.conditionType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AchievementEvaluatorRegistryTest {

    @Test
    void evaluate_evaluatorExists_returnsValue() {
        AchievementEvaluator mockEvaluator = mock(AchievementEvaluator.class);
        when(mockEvaluator.supports(conditionType.PROJECT_COMPLETED)).thenReturn(true);
        when(mockEvaluator.getCurrentValue(1L)).thenReturn(5);

        AchievementEvaluatorRegistry registry = new AchievementEvaluatorRegistry(List.of(mockEvaluator));

        int result = registry.evaluate(conditionType.PROJECT_COMPLETED, 1L);

        assertThat(result).isEqualTo(5);
        verify(mockEvaluator).getCurrentValue(1L);
    }

    @Test
    void evaluate_evaluatorDoesNotExist_returnsZero() {
        AchievementEvaluator mockEvaluator = mock(AchievementEvaluator.class);
        when(mockEvaluator.supports(any())).thenReturn(false);

        AchievementEvaluatorRegistry registry = new AchievementEvaluatorRegistry(List.of(mockEvaluator));

        int result = registry.evaluate(conditionType.PROJECT_CREATED, 1L);

        assertThat(result).isZero();
        verify(mockEvaluator, never()).getCurrentValue(1L);
    }
}
