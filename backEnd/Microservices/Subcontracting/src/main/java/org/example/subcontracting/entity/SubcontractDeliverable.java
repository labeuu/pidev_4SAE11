package org.example.subcontracting.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "subcontract_deliverables", indexes = {
        @Index(name = "idx_sd_subcontract", columnList = "subcontract_id"),
        @Index(name = "idx_sd_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubcontractDeliverable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subcontract_id", nullable = false)
    @JsonIgnore
    private Subcontract subcontract;

    @NotNull
    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeliverableStatus status = DeliverableStatus.PENDING;

    private LocalDate deadline;

    /** URL or path to the submitted deliverable file/link. */
    @Column(columnDefinition = "TEXT")
    private String submissionUrl;

    @Column(columnDefinition = "TEXT")
    private String submissionNote;

    private LocalDateTime submittedAt;

    @Column(columnDefinition = "TEXT")
    private String reviewNote;

    private LocalDateTime reviewedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = DeliverableStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
