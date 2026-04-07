package org.example.vendor.repository;

import org.example.vendor.entity.MatchRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchRecommendationRepository extends JpaRepository<MatchRecommendation, Long> {

    @Query("SELECT r FROM MatchRecommendation r WHERE r.targetType = :type AND r.targetId = :targetId ORDER BY r.matchScore DESC")
    List<MatchRecommendation> findByTarget(@Param("type") String targetType, @Param("targetId") Long targetId);

    List<MatchRecommendation> findByFreelancerIdOrderByMatchScoreDesc(Long freelancerId);

    void deleteByTargetTypeAndTargetId(String targetType, Long targetId);

    long countByTargetTypeAndTargetIdAndStatus(String targetType, Long targetId, MatchRecommendation.MatchStatus status);
}
