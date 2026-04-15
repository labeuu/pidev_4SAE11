package tn.esprit.meeting.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import tn.esprit.meeting.entity.MeetingSummary;
import tn.esprit.meeting.repository.MeetingSummaryRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeetingSummaryService {

    private final ChatClient chatClient;
    private final MeetingSummaryRepository summaryRepository;
    private final TranscriptService transcriptService;

    private static final String SUMMARY_PROMPT = """
            You are a professional meeting assistant for a freelance platform.
            Below is the transcript from a meeting between a client and a freelancer.

            Transcript:
            %s

            Write a concise meeting summary (under 300 words) covering:
            1. Main topics discussed
            2. Decisions made
            3. Action items and next steps
            4. Any proposed schedule changes

            Be professional and clear. Do not invent information not present in the transcript.
            """;

    /** Generate (or regenerate) an AI summary for the given meeting. */
    public MeetingSummary summarize(Long meetingId) {
        String combined = transcriptService.buildCombinedTranscript(meetingId);
        if (combined.isBlank()) {
            throw new RuntimeException("No transcript found for meeting " + meetingId);
        }

        log.info("[MeetingSummaryService] Generating summary for meeting {}", meetingId);
        String prompt = String.format(SUMMARY_PROMPT, combined);

        String summaryText = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        MeetingSummary summary = summaryRepository.findByMeetingId(meetingId)
                .orElseGet(MeetingSummary::new);
        summary.setMeetingId(meetingId);
        summary.setSummaryText(summaryText);
        return summaryRepository.save(summary);
    }

    public Optional<MeetingSummary> getSummary(Long meetingId) {
        return summaryRepository.findByMeetingId(meetingId);
    }
}
