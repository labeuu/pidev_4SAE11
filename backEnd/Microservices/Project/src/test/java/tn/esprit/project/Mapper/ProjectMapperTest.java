package tn.esprit.project.Mapper;

import org.junit.jupiter.api.Test;
import tn.esprit.project.Dto.request.ProjectRequest;
import tn.esprit.project.Dto.response.ProjectResponse;
import tn.esprit.project.Entities.Enums.ProjectStatus;
import tn.esprit.project.Entities.Project;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectMapperTest {

    @Test
    void toEntity_nominalCase_mapsAllFields() {
        // Arrange
        ProjectRequest request = new ProjectRequest();
        request.setClientId(1L);
        request.setTitle("Title");
        request.setDescription("Desc");
        request.setBudget(new BigDecimal("1000.0"));
        LocalDateTime deadLine = LocalDateTime.now();
        request.setDeadline(deadLine);
        request.setCategory("IT");
        request.setSkillIds(List.of(10L, 20L));

        // Act
        Project mapped = ProjectMapper.toEntity(request);

        // Assert
        assertThat(mapped).isNotNull();
        assertThat(mapped.getClientId()).isEqualTo(1L);
        assertThat(mapped.getTitle()).isEqualTo("Title");
        assertThat(mapped.getDescription()).isEqualTo("Desc");
        assertThat(mapped.getBudget()).isEqualTo(new BigDecimal("1000.0"));
        assertThat(mapped.getDeadline()).isEqualTo(deadLine);
        assertThat(mapped.getCategory()).isEqualTo("IT");
        assertThat(mapped.getSkillIds()).containsExactly(10L, 20L);
    }

    @Test
    void toDTO_nominalCase_mapsAllFields() {
        // Arrange
        Project project = new Project();
        project.setId(100L);
        project.setClientId(1L);
        project.setTitle("Title");
        project.setDescription("Desc");
        project.setBudget(new BigDecimal("1000.0"));
        LocalDateTime deadLine = LocalDateTime.now();
        project.setDeadline(deadLine);
        project.setStatus(ProjectStatus.OPEN);
        project.setCategory("IT");
        project.setSkillIds(List.of(10L, 20L));

        // Act
        ProjectResponse mapped = ProjectMapper.toDTO(project);

        // Assert
        assertThat(mapped).isNotNull();
        assertThat(mapped.getId()).isEqualTo(100L);
        assertThat(mapped.getClientId()).isEqualTo(1L);
        assertThat(mapped.getTitle()).isEqualTo("Title");
        assertThat(mapped.getDescription()).isEqualTo("Desc");
        assertThat(mapped.getBudget()).isEqualTo(new BigDecimal("1000.0"));
        assertThat(mapped.getDeadline()).isEqualTo(deadLine);
        assertThat(mapped.getStatus()).isEqualTo("OPEN");
        assertThat(mapped.getCategory()).isEqualTo("IT");
        assertThat(mapped.getSkillIds()).containsExactly(10L, 20L);
        assertThat(mapped.getSkills()).isEmpty(); // the mapper produces an empty list
    }
}
