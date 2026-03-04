package tn.esprit.project.Entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import tn.esprit.project.Entities.Enums.ProjectStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    Long clientId;
    String title;
    String description;
    BigDecimal budget;
    LocalDateTime deadline;
    ProjectStatus status;
    String category;
    @ElementCollection
    @CollectionTable(
            name = "project_skill_ids",
            joinColumns = @JoinColumn(name = "project_id")
    )
    @Column(name = "skill_id")
    private List<Long> skillIds = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    LocalDateTime updatedAt;

    @OneToMany(
            mappedBy = "project"
    )
    @JsonIgnore
    private List<ProjectApplication> applications = new ArrayList<>();




}
