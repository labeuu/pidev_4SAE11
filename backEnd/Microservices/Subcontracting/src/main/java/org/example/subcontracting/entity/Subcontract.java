package org.example.subcontracting.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "subcontracts", indexes = {
        @Index(name = "idx_sc_main_freelancer", columnList = "mainFreelancerId"),
        @Index(name = "idx_sc_subcontractor", columnList = "subcontractorId"),
        @Index(name = "idx_sc_project", columnList = "projectId"),
        @Index(name = "idx_sc_offer", columnList = "offerId"),
        @Index(name = "idx_sc_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Subcontract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false)
    private Long mainFreelancerId;

    @NotNull
    @Column(nullable = false)
    private Long subcontractorId;

    /** Mission côté microservice Project (null si la mission est une offre uniquement). */
    @Column(name = "project_id", nullable = true)
    private Long projectId;

    /** Mission côté microservice Offer (null si la mission est un projet uniquement). */
    @Column(name = "offer_id", nullable = true)
    private Long offerId;

    private Long contractId;

    @NotNull
    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String scope;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubcontractCategory category;

    @Positive
    @Column(precision = 10, scale = 2)
    private BigDecimal budget;

    @Column(length = 10)
    private String currency = "TND";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubcontractStatus status = SubcontractStatus.DRAFT;

    private LocalDate startDate;

    private LocalDate deadline;

    /** JSON array of required skill labels, e.g. ["Spring Boot","React"] */
    @Column(name = "required_skills", columnDefinition = "TEXT")
    private String requiredSkillsJson;

    @Column(name = "media_url", columnDefinition = "TEXT")
    private String mediaUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", length = 10)
    private SubcontractMediaType mediaType;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(nullable = false)
    private Integer negotiationRoundCount = 0;

    @Column(length = 30)
    private String negotiationStatus;

    @Column(precision = 10, scale = 2)
    private BigDecimal counterOfferBudget;

    private Integer counterOfferDurationDays;

    @Column(columnDefinition = "TEXT")
    private String counterOfferNote;

    @Column(precision = 10, scale = 2)
    private BigDecimal aiCompromiseBudget;

    private Integer aiCompromiseDurationDays;

    @Column(columnDefinition = "TEXT")
    private String aiCompromiseJustification;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime statusChangedAt;

    @OneToMany(mappedBy = "subcontract", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SubcontractDeliverable> deliverables = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        statusChangedAt = LocalDateTime.now();
        if (status == null) status = SubcontractStatus.DRAFT;
        if (negotiationRoundCount == null) negotiationRoundCount = 0;
        if (negotiationStatus == null) negotiationStatus = "NONE";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean canTransitionTo(SubcontractStatus target) {
        return switch (this.status) {
            case DRAFT -> target == SubcontractStatus.PROPOSED || target == SubcontractStatus.CANCELLED;
            case PROPOSED -> target == SubcontractStatus.ACCEPTED
                    || target == SubcontractStatus.REJECTED
                    || target == SubcontractStatus.COUNTER_OFFERED;
            case COUNTER_OFFERED -> target == SubcontractStatus.AI_MEDIATION
                    || target == SubcontractStatus.NEGOTIATION_IMPASSE;
            case AI_MEDIATION -> target == SubcontractStatus.NEGOTIATED
                    || target == SubcontractStatus.NEGOTIATION_IMPASSE
                    || target == SubcontractStatus.COUNTER_OFFERED;
            case NEGOTIATED -> target == SubcontractStatus.ACCEPTED
                    || target == SubcontractStatus.COUNTER_OFFERED
                    || target == SubcontractStatus.REJECTED;
            case NEGOTIATION_IMPASSE -> target == SubcontractStatus.REJECTED;
            case ACCEPTED -> target == SubcontractStatus.IN_PROGRESS || target == SubcontractStatus.CANCELLED;
            case REJECTED -> target == SubcontractStatus.DRAFT;
            case IN_PROGRESS -> target == SubcontractStatus.COMPLETED || target == SubcontractStatus.CANCELLED;
            case COMPLETED -> target == SubcontractStatus.CLOSED;
            case CANCELLED, CLOSED -> false;
        };
    }
}
