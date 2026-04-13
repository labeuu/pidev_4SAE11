package org.example.contract.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "conflicts")
public class Conflict {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    private Long raisedById;

    private String reason;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "evidence_url", columnDefinition = "TEXT")
    private String evidenceUrl;

    @Enumerated(EnumType.STRING)
    private ConflictStatus status;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime resolvedAt;

    @Column(columnDefinition = "TEXT")
    private String resolution;

    public Conflict() {}

    public Conflict(Long id, Contract contract, Long raisedById, String reason,
                    String description, String evidenceUrl, ConflictStatus status,
                    LocalDateTime createdAt, LocalDateTime resolvedAt, String resolution) {
        this.id = id;
        this.contract = contract;
        this.raisedById = raisedById;
        this.reason = reason;
        this.description = description;
        this.evidenceUrl = evidenceUrl;
        this.status = status;
        this.createdAt = createdAt;
        this.resolvedAt = resolvedAt;
        this.resolution = resolution;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = ConflictStatus.OPEN;
        }
    }

    // ── Getters ──────────────────────────────────────────────────────

    public Long getId() { return id; }
    public Contract getContract() { return contract; }
    public Long getRaisedById() { return raisedById; }
    public String getReason() { return reason; }
    public String getDescription() { return description; }
    public String getEvidenceUrl() { return evidenceUrl; }
    public ConflictStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public String getResolution() { return resolution; }

    // ── Setters ──────────────────────────────────────────────────────

    public void setId(Long id) { this.id = id; }
    public void setContract(Contract contract) { this.contract = contract; }
    public void setRaisedById(Long raisedById) { this.raisedById = raisedById; }
    public void setReason(String reason) { this.reason = reason; }
    public void setDescription(String description) { this.description = description; }
    public void setEvidenceUrl(String evidenceUrl) { this.evidenceUrl = evidenceUrl; }
    public void setStatus(ConflictStatus status) { this.status = status; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
    public void setResolution(String resolution) { this.resolution = resolution; }

    @Override
    public String toString() {
        return "Conflict{id=" + id + ", reason='" + reason + "', status=" + status + "}";
    }
}
