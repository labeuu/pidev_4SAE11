package com.esprit.portfolio.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.esprit.portfolio.dto.SkillDomainStatDto;
import com.esprit.portfolio.dto.SkillSuccessStatDto;
import com.esprit.portfolio.dto.SkillUsageStatDto;
import com.esprit.portfolio.entity.Domain;
import com.esprit.portfolio.entity.Evaluation;
import com.esprit.portfolio.entity.EvaluationTest;
import com.esprit.portfolio.entity.Experience;
import com.esprit.portfolio.entity.Skill;
import com.esprit.portfolio.repository.EvaluationRepository;
import com.esprit.portfolio.repository.EvaluationTestRepository;
import com.esprit.portfolio.repository.ExperienceRepository;
import com.esprit.portfolio.repository.SkillRepository;

@ExtendWith(MockitoExtension.class)
class SkillServiceTest {

    @Mock
    private SkillRepository skillRepository;
    @Mock
    private ExperienceRepository experienceRepository;
    @Mock
    private EvaluationTestRepository evaluationTestRepository;
    @Mock
    private EvaluationRepository evaluationRepository;

    @InjectMocks
    private SkillService skillService;

    @Test
    void getAllDomainsListsEnums() {
        assertEquals(Domain.values().length, skillService.getAllDomains().size());
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(skillRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> skillService.findById(1L));
    }

    @Test
    void createValidatesDomains() {
        Skill s = new Skill();
        s.setName("N");
        s.setUserId(1L);
        s.setDomains(List.of());
        assertThrows(IllegalArgumentException.class, () -> skillService.create(s));
    }

    @Test
    void createSavesWhenUnique() {
        Skill s = Skill.builder().name("Java").userId(2L).domains(List.of(Domain.WEB_DEVELOPMENT)).build();
        when(skillRepository.findByNameAndUserId("Java", 2L)).thenReturn(Optional.empty());
        when(skillRepository.save(any(Skill.class))).thenAnswer(inv -> inv.getArgument(0));
        Skill out = skillService.create(s);
        assertEquals("Java", out.getName());
    }

    @Test
    void updateSkillDomains() {
        Skill db = new Skill();
        db.setId(5L);
        db.setName("X");
        db.setUserId(1L);
        db.setDomains(new ArrayList<>(List.of(Domain.WEB_DEVELOPMENT)));
        when(skillRepository.findById(5L)).thenReturn(Optional.of(db));
        when(skillRepository.save(any(Skill.class))).thenAnswer(inv -> inv.getArgument(0));
        Skill out = skillService.updateSkillDomains(5L, List.of(Domain.VIDEO_MAKING));
        assertEquals(1, out.getDomains().size());
    }

    @Test
    void deleteRemovesRelations() {
        Skill sk = new Skill();
        sk.setId(9L);
        when(skillRepository.findById(9L)).thenReturn(Optional.of(sk));
        Experience exp = new Experience();
        exp.setSkills(new ArrayList<>(List.of(sk)));
        when(experienceRepository.findBySkills_Id(9L)).thenReturn(List.of(exp));
        when(evaluationTestRepository.findBySkillId(9L)).thenReturn(List.of(new EvaluationTest()));
        when(evaluationRepository.findBySkillId(9L)).thenReturn(List.of(new Evaluation()));
        skillService.delete(9L);
        verify(skillRepository).deleteById(9L);
    }

    @Test
    void getSkillUsageStatsEmptyWhenNoUsers() {
        when(skillRepository.countUsersBySkillName()).thenReturn(List.of());
        when(skillRepository.countDistinctUsers()).thenReturn(0L);
        assertEquals(0, skillService.getSkillUsageStats().size());
    }

    @Test
    void getSkillUsageStatsMapsRows() {
        when(skillRepository.countUsersBySkillName())
                .thenReturn(Collections.singletonList(new Object[]{"Java", 3L}));
        when(skillRepository.countDistinctUsers()).thenReturn(10L);
        List<SkillUsageStatDto> stats = skillService.getSkillUsageStats();
        assertEquals(1, stats.size());
        assertEquals("Java", stats.get(0).skillName());
    }

    @Test
    void getSkillStatsByDomainDelegates() {
        when(skillRepository.countSkillsGroupedByDomain())
                .thenReturn(List.of(new SkillDomainStatDto(Domain.WEB_DEVELOPMENT, 3L)));
        assertEquals(1, skillService.getSkillStatsByDomain().size());
    }

    @Test
    void findAllByIdsEmptyReturnsEmpty() {
        assertEquals(0, skillService.findAllByIds(List.of()).size());
    }

    @Test
    void findAllByIdsDelegates() {
        when(skillRepository.findAllById(anyList())).thenReturn(List.of(new Skill()));
        assertEquals(1, skillService.findAllByIds(List.of(1L)).size());
    }

    @Test
    void getSkillSuccessStatsMapsRows() {
        Object[] row = new Object[]{"SkillA", 10L, 7L};
        when(evaluationRepository.countEvaluationsBySkill()).thenReturn(Collections.singletonList(row));
        List<SkillSuccessStatDto> stats = skillService.getSkillSuccessStats();
        assertEquals(1, stats.size());
        assertEquals("SkillA", stats.get(0).skillName());
    }
}
