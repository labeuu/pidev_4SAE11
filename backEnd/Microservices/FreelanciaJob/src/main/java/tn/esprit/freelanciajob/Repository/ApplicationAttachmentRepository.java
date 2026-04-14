package tn.esprit.freelanciajob.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.freelanciajob.Entity.ApplicationAttachment;

import java.util.List;

public interface ApplicationAttachmentRepository extends JpaRepository<ApplicationAttachment, Long> {

    List<ApplicationAttachment> findByJobApplicationId(Long jobApplicationId);

    void deleteByJobApplicationId(Long jobApplicationId);
}
