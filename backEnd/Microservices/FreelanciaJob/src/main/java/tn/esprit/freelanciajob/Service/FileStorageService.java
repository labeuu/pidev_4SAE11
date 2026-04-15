package tn.esprit.freelanciajob.Service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Abstraction for file-persistence operations.
 * Swap the implementation (local disk → S3, GCS, etc.) without touching business code.
 */
public interface FileStorageService {

    /**
     * Validates a batch of files against business rules (count, size, type).
     * Throws {@link RuntimeException} with a user-friendly message on violation.
     */
    void validateFiles(List<MultipartFile> files);

    /**
     * Persists a single file and returns its relative URL path.
     *
     * @param file          the uploaded file
     * @param applicationId used to build the storage sub-directory
     * @return relative URL, e.g. {@code /uploads/applications/42/uuid.pdf}
     */
    String storeFile(MultipartFile file, Long applicationId);

    /**
     * Deletes all files belonging to an application (for cleanup on delete).
     *
     * @param applicationId sub-directory to remove
     */
    void deleteApplicationFiles(Long applicationId);
}
