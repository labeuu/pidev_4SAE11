package tn.esprit.meeting.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.meeting.entity.MeetingTranscript;

import java.util.List;
import java.util.Optional;

public interface MeetingTranscriptRepository extends JpaRepository<MeetingTranscript, Long> {
    List<MeetingTranscript> findByMeetingIdOrderBySavedAtAsc(Long meetingId);
    Optional<MeetingTranscript> findByMeetingIdAndUserId(Long meetingId, Long userId);
}
