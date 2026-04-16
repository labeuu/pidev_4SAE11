package tn.esprit.meeting.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "meeting_transcripts",
       uniqueConstraints = @UniqueConstraint(columnNames = {"meetingId", "userId"}))
@Getter
@Setter
public class MeetingTranscript {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long meetingId;

    @Column(nullable = false)
    private Long userId;

    private String userName;

    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String content;

    private LocalDateTime savedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        savedAt = LocalDateTime.now();
    }
}
