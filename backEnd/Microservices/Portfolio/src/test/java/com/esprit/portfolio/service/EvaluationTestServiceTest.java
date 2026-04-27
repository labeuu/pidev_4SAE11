package com.esprit.portfolio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.esprit.portfolio.entity.EvaluationTest;
import com.esprit.portfolio.entity.Question;
import com.esprit.portfolio.entity.Skill;
import com.esprit.portfolio.repository.EvaluationTestRepository;
import com.esprit.portfolio.repository.SkillRepository;

@ExtendWith(MockitoExtension.class)
class EvaluationTestServiceTest {

    @Mock
    private EvaluationTestRepository evaluationTestRepository;

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private AIService aiService;

    @InjectMocks
    private EvaluationTestService evaluationTestService;

    private Skill skill;

    @BeforeEach
    void setUp() {
        skill = Skill.builder().id(10L).name("Java").build();
    }

    @Test
    void findAllDelegatesToRepository() {
        when(evaluationTestRepository.findAll()).thenReturn(List.of());
        assertThat(evaluationTestService.findAll()).isEmpty();
    }

    @Test
    void findByIdReturnsEntity() {
        EvaluationTest t = EvaluationTest.builder().id(1L).title("T").build();
        when(evaluationTestRepository.findById(1L)).thenReturn(Optional.of(t));
        assertThat(evaluationTestService.findById(1L)).isSameAs(t);
    }

    @Test
    void findByIdMissingThrows() {
        when(evaluationTestRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> evaluationTestService.findById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void findBySkillIdDelegates() {
        when(evaluationTestRepository.findBySkillId(5L)).thenReturn(List.of());
        assertThat(evaluationTestService.findBySkillId(5L)).isEmpty();
    }

    @Test
    void createSetsSkillAndSaves() {
        EvaluationTest input = new EvaluationTest();
        EvaluationTest saved = EvaluationTest.builder().id(3L).title("X").build();
        when(skillRepository.findById(10L)).thenReturn(Optional.of(skill));
        when(evaluationTestRepository.save(any(EvaluationTest.class))).thenReturn(saved);

        EvaluationTest result = evaluationTestService.create(input, 10L);

        assertThat(result.getId()).isEqualTo(3L);
        assertThat(input.getSkill()).isEqualTo(skill);
    }

    @Test
    void createWhenSkillMissingThrows() {
        when(skillRepository.findById(10L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> evaluationTestService.create(new EvaluationTest(), 10L))
                .hasMessageContaining("Skill not found");
    }

    @Test
    void generateTestForSkillBuildsAndSaves() {
        Question q = new Question();
        q.setPoints(2);
        when(skillRepository.findById(10L)).thenReturn(Optional.of(skill));
        when(aiService.generateQuestions("Java")).thenReturn(List.of(q));
        EvaluationTest saved = EvaluationTest.builder().id(7L).build();
        when(evaluationTestRepository.save(any(EvaluationTest.class))).thenReturn(saved);

        EvaluationTest result = evaluationTestService.generateTestForSkill(10L);

        assertThat(result.getId()).isEqualTo(7L);
        verify(evaluationTestRepository).save(any(EvaluationTest.class));
    }

    @Test
    void generateTestForSkillWhenSkillMissingThrows() {
        when(skillRepository.findById(10L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> evaluationTestService.generateTestForSkill(10L))
                .hasMessageContaining("Skill not found");
    }

    @Test
    void generateTestForSkillWhenNoQuestionsThrows() {
        when(skillRepository.findById(10L)).thenReturn(Optional.of(skill));
        when(aiService.generateQuestions("Java")).thenReturn(List.of());
        assertThatThrownBy(() -> evaluationTestService.generateTestForSkill(10L))
                .hasMessageContaining("Failed to generate");
    }

    @Test
    void updateMergesDetails() {
        EvaluationTest existing = EvaluationTest.builder().id(1L).title("Old").build();
        when(evaluationTestRepository.findById(1L)).thenReturn(Optional.of(existing));
        EvaluationTest details = new EvaluationTest();
        details.setTitle("New");
        details.setPassingScore(5.0);
        details.setDurationMinutes(20);
        when(evaluationTestRepository.save(any(EvaluationTest.class))).thenAnswer(inv -> inv.getArgument(0));

        EvaluationTest out = evaluationTestService.update(1L, details);

        assertThat(out.getTitle()).isEqualTo("New");
        assertThat(out.getPassingScore()).isEqualTo(5.0);
        assertThat(out.getDurationMinutes()).isEqualTo(20);
    }

    @Test
    void updateCopiesQuestionsWhenPresent() {
        EvaluationTest existing = EvaluationTest.builder().id(1L).title("Old").build();
        when(evaluationTestRepository.findById(1L)).thenReturn(Optional.of(existing));
        Question q = new Question();
        EvaluationTest details = new EvaluationTest();
        details.setTitle("N");
        details.setQuestions(List.of(q));
        when(evaluationTestRepository.save(any(EvaluationTest.class))).thenAnswer(inv -> inv.getArgument(0));

        EvaluationTest out = evaluationTestService.update(1L, details);

        assertThat(out.getQuestions()).containsExactly(q);
    }

    @Test
    void deleteWhenExists() {
        when(evaluationTestRepository.existsById(2L)).thenReturn(true);
        evaluationTestService.delete(2L);
        verify(evaluationTestRepository).deleteById(2L);
    }

    @Test
    void deleteWhenMissingThrows() {
        when(evaluationTestRepository.existsById(2L)).thenReturn(false);
        assertThatThrownBy(() -> evaluationTestService.delete(2L))
                .hasMessageContaining("not found");
    }
}
