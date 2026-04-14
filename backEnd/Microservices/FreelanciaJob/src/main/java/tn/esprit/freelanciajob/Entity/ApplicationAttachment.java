package tn.esprit.freelanciajob.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * One-to-many child of JobApplication.
 * Stores metadata for every file uploaded with an application.
 * Files are physically stored on disk; this table tracks the reference.
 */
@Entity
@Table(name = "application_attachments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK – matches job_applications.id; intentionally not a JPA relationship
     *  so that JobApplication.java remains untouched. */
    @Column(name = "job_application_id", nullable = false)
    private Long jobApplicationId;

    /** Original filename supplied by the user (for display). */
    @Column(nullable = false)
    private String fileName;

    /** MIME content-type (e.g. "application/pdf"). */
    @Column(nullable = false, length = 100)
    private String fileType;

    /** Relative URL path (e.g. /uploads/applications/42/uuid.pdf).
     *  Frontend prepends the gateway base-URL to get the full download link. */
    @Column(nullable = false)
    private String fileUrl;

    /** Size in bytes. */
    @Column(nullable = false)
    private Long fileSize;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadedAt;
}
