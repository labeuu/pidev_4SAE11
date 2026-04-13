package org.example.contract.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "contracts")
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long clientId;
    private Long freelancerId;
    private Long projectApplicationId;
    private Long offerApplicationId;
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String terms;

    @Column(name = "client_signature_data", columnDefinition = "MEDIUMTEXT")
    private String clientSignatureUrl;

    @Column(name = "freelancer_signature_data", columnDefinition = "MEDIUMTEXT")
    private String freelancerSignatureUrl;

    private BigDecimal amount;
    private LocalDate startDate;
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private ContractStatus status;

    private LocalDateTime signedAt;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @JsonIgnore
    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL)
    private List<Conflict> conflicts;

    public Contract() {}

    public Contract(Long id, Long clientId, Long freelancerId, Long projectApplicationId,
                    Long offerApplicationId, String title, String description, String terms,
                    String clientSignatureUrl, String freelancerSignatureUrl,
                    BigDecimal amount, LocalDate startDate, LocalDate endDate,
                    ContractStatus status, LocalDateTime signedAt, LocalDateTime createdAt,
                    List<Conflict> conflicts) {
        this.id = id;
        this.clientId = clientId;
        this.freelancerId = freelancerId;
        this.projectApplicationId = projectApplicationId;
        this.offerApplicationId = offerApplicationId;
        this.title = title;
        this.description = description;
        this.terms = terms;
        this.clientSignatureUrl = clientSignatureUrl;
        this.freelancerSignatureUrl = freelancerSignatureUrl;
        this.amount = amount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.signedAt = signedAt;
        this.createdAt = createdAt;
        this.conflicts = conflicts;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = ContractStatus.DRAFT;
        }
    }

    // ── Getters ──────────────────────────────────────────────────────

    public Long getId() { return id; }
    public Long getClientId() { return clientId; }
    public Long getFreelancerId() { return freelancerId; }
    public Long getProjectApplicationId() { return projectApplicationId; }
    public Long getOfferApplicationId() { return offerApplicationId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getTerms() { return terms; }
    public String getClientSignatureUrl() { return clientSignatureUrl; }
    public String getFreelancerSignatureUrl() { return freelancerSignatureUrl; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public ContractStatus getStatus() { return status; }
    public LocalDateTime getSignedAt() { return signedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<Conflict> getConflicts() { return conflicts; }

    // ── Setters ──────────────────────────────────────────────────────

    public void setId(Long id) { this.id = id; }
    public void setClientId(Long clientId) { this.clientId = clientId; }
    public void setFreelancerId(Long freelancerId) { this.freelancerId = freelancerId; }
    public void setProjectApplicationId(Long projectApplicationId) { this.projectApplicationId = projectApplicationId; }
    public void setOfferApplicationId(Long offerApplicationId) { this.offerApplicationId = offerApplicationId; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setTerms(String terms) { this.terms = terms; }
    public void setClientSignatureUrl(String clientSignatureUrl) { this.clientSignatureUrl = clientSignatureUrl; }
    public void setFreelancerSignatureUrl(String freelancerSignatureUrl) { this.freelancerSignatureUrl = freelancerSignatureUrl; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public void setStatus(ContractStatus status) { this.status = status; }
    public void setSignedAt(LocalDateTime signedAt) { this.signedAt = signedAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setConflicts(List<Conflict> conflicts) { this.conflicts = conflicts; }

    @Override
    public String toString() {
        return "Contract{id=" + id + ", title='" + title + "', status=" + status + "}";
    }
}
