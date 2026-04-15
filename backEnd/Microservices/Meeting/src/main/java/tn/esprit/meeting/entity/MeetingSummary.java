package tn.esprit.meeting.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "meeting_summaries")
@Getter
@Setter
public class MeetingSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long meetingId;

    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String summaryText;

    private LocalDateTime generatedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        generatedAt = LocalDateTime.now();
    }
}
