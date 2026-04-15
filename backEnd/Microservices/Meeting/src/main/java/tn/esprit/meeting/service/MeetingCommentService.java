package tn.esprit.meeting.service;

import org.springframework.stereotype.Service;
import tn.esprit.meeting.entity.MeetingComment;
import tn.esprit.meeting.repository.MeetingCommentRepository;

import java.util.List;

@Service
public class MeetingCommentService {

    private final MeetingCommentRepository repository;

    public MeetingCommentService(MeetingCommentRepository repository) {
        this.repository = repository;
    }

    public List<MeetingComment> getByMeetingId(Long meetingId) {
        return repository.findByMeetingIdOrderByCreatedAtAsc(meetingId);
    }

    public MeetingComment create(Long meetingId, MeetingComment comment) {
        comment.setMeetingId(meetingId);
        return repository.save(comment);
    }

    public MeetingComment update(Long id, String content) {
        MeetingComment comment = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found: " + id));
        comment.setContent(content);
        return repository.save(comment);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
