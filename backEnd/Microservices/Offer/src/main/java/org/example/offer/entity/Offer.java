package org.example.offer.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "offers", indexes = {
        @Index(name = "idx_freelancer_id", columnList = "freelancerId"),
        @Index(name = "idx_offer_status", columnList = "offerStatus"),
        @Index(name = "idx_rating", columnList = "rating"),
        @Index(name = "idx_domain", columnList = "domain")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Freelancer ID is required")
    @Column(nullable = false)
    private Long freelancerId;

    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 255)
    @Column(nullable = false, length = 255)
    private String title;

    @NotBlank(message = "Domain is required")
    @Column(nullable = false, length = 100)
    private String domain;

    @NotBlank(message = "Description is required")
    @Size(min = 20, max = 5000)
    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false)
    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal price;

    @NotBlank(message = "Duration type is required")
    @Pattern(regexp = "hourly|fixed|monthly")
    @Column(nullable = false, length = 50)
    private String durationType;

    @NotNull(message = "Offer status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, name = "offerStatus")
    private OfferStatus offerStatus;

    // ✅ SOLUTION : Stocker uniquement l'ID du ProjectStatus (Foreign Key logique)
    // PAS de relation JPA @ManyToOne car ProjectStatus est dans un autre microservice
    @Column(name = "project_status_id")
    private Long projectStatusId;

    private LocalDate deadline;

    @Column(length = 100)
    private String category;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "5.0")
    @Column(precision = 3, scale = 2)
    private BigDecimal rating;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "5.0")
    @Column(precision = 3, scale = 2)
    private BigDecimal communicationScore;

    @Column(length = 500)
    private String tags;

    @Column(length = 255)
    private String imageUrl;

    @Column(nullable = false)
    private Integer viewsCount = 0;

    @Column(nullable = false)
    private Boolean isFeatured = false;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime publishedAt;

    private LocalDateTime expiredAt;

    @OneToMany(mappedBy = "offer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OfferApplication> applications = new ArrayList<>();

    // ProjectApplication belongs to the Project microservice - no JPA relation across services.
    // Use the Project API if you need applications linked to this offer.

    // Review belongs to the Review microservice - no JPA relation across services.
    // Use the Review API (e.g. by offerId if exposed) to get reviews for this offer.

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        if (offerStatus == null) {
            offerStatus = OfferStatus.DRAFT;
        }
        if (rating == null) {
            rating = BigDecimal.ZERO;
        }
        if (communicationScore == null) {
            communicationScore = BigDecimal.ZERO;
        }
        if (viewsCount == null) {
            viewsCount = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Méthodes métier
    public void publish() {
        if (this.offerStatus == OfferStatus.DRAFT) {
            this.offerStatus = OfferStatus.AVAILABLE;
            this.publishedAt = LocalDateTime.now();
            this.isActive = true;
        }
    }

    public void deactivate() {
        this.isActive = false;
        this.offerStatus = OfferStatus.CLOSED;
    }

    public void expire() {
        this.offerStatus = OfferStatus.EXPIRED;
        this.expiredAt = LocalDateTime.now();
        this.isActive = false;
    }

    public void accept() {
        this.offerStatus = OfferStatus.ACCEPTED;
        this.isActive = false;
    }

    public void incrementViews() {
        this.viewsCount++;
    }

    public int getApplicationsCount() {
        return applications != null ? applications.size() : 0;
    }

    public long getPendingApplicationsCount() {
        return applications != null
                ? applications.stream()
                .filter(app -> app.getStatus() == ApplicationStatus.PENDING)
                .count()
                : 0;
    }

    public boolean isValid() {
        if (deadline == null) return true;
        return LocalDate.now().isBefore(deadline) || LocalDate.now().isEqual(deadline);
    }

    public boolean canReceiveApplications() {
        return this.isActive
                && this.offerStatus == OfferStatus.AVAILABLE
                && isValid();
    }
}