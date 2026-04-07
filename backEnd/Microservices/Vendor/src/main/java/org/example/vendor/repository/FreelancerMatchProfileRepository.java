package org.example.vendor.repository;

import org.example.vendor.entity.FreelancerMatchProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FreelancerMatchProfileRepository extends JpaRepository<FreelancerMatchProfile, Long> {

    Optional<FreelancerMatchProfile> findByFreelancerId(Long freelancerId);

    @Query("SELECT p FROM FreelancerMatchProfile p ORDER BY p.globalScore DESC")
    List<FreelancerMatchProfile> findTopRanked();

    @Query("SELECT p FROM FreelancerMatchProfile p WHERE p.vendorBoosted = true ORDER BY p.globalScore DESC")
    List<FreelancerMatchProfile> findVendorBoosted();

    @Query("SELECT p FROM FreelancerMatchProfile p WHERE LOWER(p.skillTags) LIKE LOWER(CONCAT('%', :skill, '%')) ORDER BY p.globalScore DESC")
    List<FreelancerMatchProfile> findBySkillMatch(@Param("skill") String skill);

    void deleteByFreelancerId(Long freelancerId);
}
