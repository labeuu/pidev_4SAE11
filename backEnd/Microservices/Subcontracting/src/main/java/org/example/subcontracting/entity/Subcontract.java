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

    @Column(nullable = false)
    private Long projectId;

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

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(columnDefinition = "TEXT")
    private String cancellationReason;

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
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean canTransitionTo(SubcontractStatus target) {
        return switch (this.status) {
            case DRAFT -> target == SubcontractStatus.PROPOSED || target == SubcontractStatus.CANCELLED;
            case PROPOSED -> target == SubcontractStatus.ACCEPTED || target == SubcontractStatus.REJECTED;
            case ACCEPTED -> target == SubcontractStatus.IN_PROGRESS || target == SubcontractStatus.CANCELLED;
            case REJECTED -> target == SubcontractStatus.DRAFT;
            case IN_PROGRESS -> target == SubcontractStatus.COMPLETED || target == SubcontractStatus.CANCELLED;
            case COMPLETED -> target == SubcontractStatus.CLOSED;
            case CANCELLED, CLOSED -> false;
        };
    }
}
