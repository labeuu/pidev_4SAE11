package tn.esprit.meeting.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.meeting.entity.Meeting;
import tn.esprit.meeting.enums.MeetingStatus;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    /** All meetings where the user is either CLIENT or FREELANCER, newest first */
    @Query("SELECT m FROM Meeting m WHERE m.clientId = :userId OR m.freelancerId = :userId ORDER BY m.startTime DESC")
    List<Meeting> findAllByUserId(@Param("userId") Long userId);

    /** Meetings owned by a specific CLIENT */
    List<Meeting> findByClientIdOrderByStartTimeDesc(Long clientId);

    /** Meetings where the user is the FREELANCER */
    List<Meeting> findByFreelancerIdOrderByStartTimeDesc(Long freelancerId);

    /** Meetings in a specific status for a user */
    @Query("SELECT m FROM Meeting m WHERE (m.clientId = :userId OR m.freelancerId = :userId) AND m.status = :status ORDER BY m.startTime ASC")
    List<Meeting> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") MeetingStatus status);

    /** Upcoming meetings (start > now, status = ACCEPTED) */
    @Query("SELECT m FROM Meeting m WHERE (m.clientId = :userId OR m.freelancerId = :userId) AND m.status = 'ACCEPTED' AND m.startTime > :now ORDER BY m.startTime ASC")
    List<Meeting> findUpcomingByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /** Past meetings that need to be marked COMPLETED */
    @Query("SELECT m FROM Meeting m WHERE m.status = 'ACCEPTED' AND m.endTime < :now")
    List<Meeting> findCompletable(@Param("now") LocalDateTime now);

    /** Check for scheduling conflicts for a user */
    @Query("SELECT m FROM Meeting m WHERE (m.clientId = :userId OR m.freelancerId = :userId) AND m.status IN ('PENDING','ACCEPTED') AND m.startTime < :endTime AND m.endTime > :startTime")
    List<Meeting> findConflicts(@Param("userId") Long userId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}
