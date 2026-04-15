package tn.esprit.meeting.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.meeting.entity.MeetingTranscript;
import tn.esprit.meeting.repository.MeetingTranscriptRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TranscriptService {

    private final MeetingTranscriptRepository repository;

    /** Upsert: one transcript entry per user per meeting. */
    public MeetingTranscript save(Long meetingId, Long userId, String userName, String content) {
        MeetingTranscript transcript = repository
                .findByMeetingIdAndUserId(meetingId, userId)
                .orElseGet(MeetingTranscript::new);

        transcript.setMeetingId(meetingId);
        transcript.setUserId(userId);
        transcript.setUserName(userName);
        transcript.setContent(content);
        return repository.save(transcript);
    }

    public List<MeetingTranscript> getByMeetingId(Long meetingId) {
        return repository.findByMeetingIdOrderBySavedAtAsc(meetingId);
    }

    /** Combine all participant transcripts into one labelled text block. */
    public String buildCombinedTranscript(Long meetingId) {
        List<MeetingTranscript> transcripts = getByMeetingId(meetingId);
        if (transcripts.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        for (MeetingTranscript t : transcripts) {
            sb.append("[").append(t.getUserName()).append("]\n");
            sb.append(t.getContent()).append("\n\n");
        }
        return sb.toString().trim();
    }
}
