package org.example.subcontracting.coach.repository;

import org.example.subcontracting.coach.entity.CoachFeatureCost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CoachFeatureCostRepository extends JpaRepository<CoachFeatureCost, Long> {

    Optional<CoachFeatureCost> findByFeatureCodeAndActiveIsTrue(String featureCode);

    List<CoachFeatureCost> findAllByOrderByFeatureCodeAsc();
}
