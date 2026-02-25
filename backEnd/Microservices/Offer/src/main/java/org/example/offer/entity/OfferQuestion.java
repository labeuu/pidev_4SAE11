package org.example.offer.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Question posée par un client sur une offre (Q&A avant de commander).
 * Le freelancer (propriétaire de l'offre) peut répondre.
 */
@Entity
@Table(name = "offer_questions", indexes = {
        @Index(name = "idx_offer_question_offer", columnList = "offer_id"),
        @Index(name = "idx_offer_question_client", columnList = "clientId"),
        @Index(name = "idx_offer_question_asked", columnList = "askedAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OfferQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offer_id", nullable = false)
    private Offer offer;

    @NotNull
    @Column(nullable = false)
    private Long clientId;

    @NotBlank
    @Size(min = 10, max = 1000)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Size(max = 2000)
    @Column(columnDefinition = "TEXT")
    private String answerText;

    @Column(nullable = false, updatable = false)
    private LocalDateTime askedAt;

    private LocalDateTime answeredAt;

    @PrePersist
    protected void onCreate() {
        if (askedAt == null) askedAt = LocalDateTime.now();
    }

    public boolean isAnswered() {
        return answerText != null && !answerText.isBlank();
    }
}
