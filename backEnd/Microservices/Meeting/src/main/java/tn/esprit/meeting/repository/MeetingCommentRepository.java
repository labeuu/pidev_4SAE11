package tn.esprit.meeting.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.meeting.entity.MeetingComment;

import java.util.List;

public interface MeetingCommentRepository extends JpaRepository<MeetingComment, Long> {
    List<MeetingComment> findByMeetingIdOrderByCreatedAtAsc(Long meetingId);
}
