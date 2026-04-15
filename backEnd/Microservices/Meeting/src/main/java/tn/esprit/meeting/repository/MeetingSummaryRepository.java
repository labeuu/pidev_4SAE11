package tn.esprit.meeting.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.meeting.entity.MeetingSummary;

import java.util.Optional;

public interface MeetingSummaryRepository extends JpaRepository<MeetingSummary, Long> {
    Optional<MeetingSummary> findByMeetingId(Long meetingId);
}
