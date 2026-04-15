package com.esprit.planning.repository;

import com.esprit.planning.entity.ProgressUpdate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** JPA repository for ProgressUpdate entity. Supports filtering via JpaSpecificationExecutor and custom queries for trends, stalled projects, rankings, and calendar. */
@Repository
public interface ProgressUpdateRepository extends JpaRepository<ProgressUpdate, Long>, JpaSpecificationExecutor<ProgressUpdate> {

    List<ProgressUpdate> findByProjectId(Long projectId);

    List<ProgressUpdate> findByProjectIdAndCreatedAtBetween(Long projectId, LocalDateTime from, LocalDateTime to);

    List<ProgressUpdate> findByContractId(Long contractId);

    List<ProgressUpdate> findByContractIdIn(Collection<Long> contractIds);

    List<ProgressUpdate> findByFreelancerId(Long freelancerId);

    List<ProgressUpdate> findByProjectIdIn(Collection<Long> projectIds);

    /** For stalled projects: projectId and its latest update time. */
    @Query("SELECT p.projectId, MAX(p.updatedAt) FROM ProgressUpdate p GROUP BY p.projectId")
    List<Object[]> findProjectIdAndMaxUpdatedAt();

    Optional<ProgressUpdate> findByProjectIdAndUpdatedAt(Long projectId, LocalDateTime updatedAt);

    /**
     * Latest progress update for a given project.
     */
    Optional<ProgressUpdate> findFirstByProjectIdOrderByCreatedAtDesc(Long projectId);

    /**
     * Latest progress update for a given freelancer.
     */
    Optional<ProgressUpdate> findFirstByFreelancerIdOrderByCreatedAtDesc(Long freelancerId);

    /**
     * Latest progress update for a given contract.
     */
    Optional<ProgressUpdate> findFirstByContractIdOrderByCreatedAtDesc(Long contractId);

    /** Top freelancers by update count (order by count desc, limit via Pageable). */
    @Query("SELECT p.freelancerId, COUNT(p) FROM ProgressUpdate p GROUP BY p.freelancerId ORDER BY COUNT(p) DESC")
    List<Object[]> findFreelancerIdAndUpdateCountOrderByCountDesc(Pageable pageable);

    /** Most active projects by update count, optional date range on createdAt. */
    @Query("SELECT p.projectId, COUNT(p) FROM ProgressUpdate p WHERE (:from IS NULL OR p.createdAt >= :from) AND (:to IS NULL OR p.createdAt <= :to) GROUP BY p.projectId ORDER BY COUNT(p) DESC")
    List<Object[]> findProjectIdAndUpdateCountOrderByCountDescBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);

    /** Progress updates with a next-update-due date in the given range (for calendar view when Google Calendar is disabled). */
    List<ProgressUpdate> findByNextUpdateDueBetween(LocalDateTime start, LocalDateTime end);

    /** Distinct project IDs for which the given user has progress updates as freelancer (for calendar filtering). */
    @Query("SELECT DISTINCT p.projectId FROM ProgressUpdate p WHERE p.freelancerId = :freelancerId")
    List<Long> findDistinctProjectIdsByFreelancerId(@Param("freelancerId") Long freelancerId);

    @Query("SELECT DISTINCT p.projectId FROM ProgressUpdate p WHERE p.contractId IN :contractIds")
    List<Long> findDistinctProjectIdsByContractIdIn(@Param("contractIds") Collection<Long> contractIds);

    /** Overdue next-update-due rows not yet notified (scheduler). */
    List<ProgressUpdate> findByNextUpdateDueIsNotNullAndNextUpdateDueBeforeAndNextDueOverdueNotifiedIsFalse(LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProgressUpdate p SET p.nextDueCalendarEventId = NULL WHERE p.nextUpdateDue IS NULL AND p.nextDueCalendarEventId IS NOT NULL")
    int clearOrphanNextDueCalendarEventIds();
}
