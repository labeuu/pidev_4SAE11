package tn.esprit.freelanciajob.Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final int  MAX_FILES     = 5;
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10 MB

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "image/png",
            "image/jpeg",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    @Value("${app.upload.base-dir:uploads}")
    private String uploadBaseDir;

    // ─── Validation ───────────────────────────────────────────────────────────

    @Override
    public void validateFiles(List<MultipartFile> files) {
        if (files.size() > MAX_FILES) {
            throw new RuntimeException(
                    "Too many files: maximum " + MAX_FILES + " allowed per application.");
        }
        for (MultipartFile file : files) {
            if (file.getSize() > MAX_FILE_SIZE) {
                throw new RuntimeException(
                        "File \"" + file.getOriginalFilename() + "\" exceeds the 10 MB size limit.");
            }
            String contentType = file.getContentType();
            if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
                throw new RuntimeException(
                        "File type not allowed: \"" + contentType + "\". " +
                        "Accepted types: PDF, PNG, JPG, DOC, DOCX.");
            }
        }
    }

    // ─── Storage ──────────────────────────────────────────────────────────────

    @Override
    public String storeFile(MultipartFile file, Long applicationId) {
        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
        String extension    = getExtension(originalName);
        String storedName   = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);

        Path uploadPath = Paths.get(uploadBaseDir, "applications", String.valueOf(applicationId));
        try {
            Files.createDirectories(uploadPath);
            Files.copy(file.getInputStream(), uploadPath.resolve(storedName),
                       StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            log.error("[FileStorage] Failed to store file {} for application {}: {}",
                      originalName, applicationId, ex.getMessage());
            throw new RuntimeException("Could not save file: " + originalName, ex);
        }

        String relativeUrl = "/uploads/applications/" + applicationId + "/" + storedName;
        log.info("[FileStorage] Stored {} → {}", originalName, relativeUrl);
        return relativeUrl;
    }

    // ─── Cleanup ──────────────────────────────────────────────────────────────

    @Override
    public void deleteApplicationFiles(Long applicationId) {
        Path dir = Paths.get(uploadBaseDir, "applications", String.valueOf(applicationId));
        if (!Files.exists(dir)) return;
        try (var stream = Files.walk(dir)) {
            stream.sorted(java.util.Comparator.reverseOrder())
                  .map(Path::toFile)
                  .forEach(java.io.File::delete);
            log.info("[FileStorage] Deleted all files for application {}", applicationId);
        } catch (IOException ex) {
            log.warn("[FileStorage] Could not delete files for application {}: {}", applicationId, ex.getMessage());
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return (dot >= 0 && dot < filename.length() - 1) ? filename.substring(dot + 1) : "";
    }
}
