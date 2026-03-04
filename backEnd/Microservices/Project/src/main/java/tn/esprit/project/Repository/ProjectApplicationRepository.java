package tn.esprit.project.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tn.esprit.project.Dto.ProjectApplicationStats;
import tn.esprit.project.Entities.ProjectApplication;

import java.util.List;

@Repository
public interface ProjectApplicationRepository extends JpaRepository<ProjectApplication, Long> {
    List<ProjectApplication> findByProjectId(Long projectId);
    List<ProjectApplication> findByFreelanceId(Long freelanceId);
    @Query("""
        SELECT new tn.esprit.project.Dto.ProjectApplicationStats(
            p.id,
            p.title,
            COUNT(pa)
        )
        FROM ProjectApplication pa
        RIGHT JOIN pa.project p
        GROUP BY p.id, p.title
    """)
    List<ProjectApplicationStats> getApplicationsStatistics();
}
