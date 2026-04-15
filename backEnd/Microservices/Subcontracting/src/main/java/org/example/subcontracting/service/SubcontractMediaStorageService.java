package org.example.subcontracting.service;

import lombok.extern.slf4j.Slf4j;
import org.example.subcontracting.entity.SubcontractMediaType;
import org.example.subcontracting.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class SubcontractMediaStorageService {

    private static final long MAX_BYTES = 120L * 1024 * 1024; // ~120 Mo (vidéo courte HD)

    private static final Set<String> MP4_TYPES = Set.of("video/mp4", "application/mp4");
    private static final Set<String> MP3_TYPES = Set.of("audio/mpeg", "audio/mp3");

    private final Path rootDirectory;

    public SubcontractMediaStorageService(
            @Value("${app.subcontract.media.storage-path:./data/subcontract-media}") String storagePath) {
        this.rootDirectory = Paths.get(storagePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootDirectory);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de créer le répertoire média: " + this.rootDirectory, e);
        }
    }

    /**
     * Enregistre le fichier et retourne le nom stocké (UUID + extension).
     */
    public StoredMedia save(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Fichier média vide");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BadRequestException("Fichier trop volumineux (max ~120 Mo)");
        }

        String contentType = file.getContentType();
        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        String lower = original.toLowerCase(Locale.ROOT);

        SubcontractMediaType mediaType;
        String ext;
        if (MP4_TYPES.contains(contentType) || lower.endsWith(".mp4")) {
            mediaType = SubcontractMediaType.VIDEO;
            ext = ".mp4";
        } else if (MP3_TYPES.contains(contentType) || lower.endsWith(".mp3")) {
            mediaType = SubcontractMediaType.AUDIO;
            ext = ".mp3";
        } else {
            throw new BadRequestException("Format non supporté : uniquement MP4 (vidéo) ou MP3 (audio)");
        }

        String storedName = UUID.randomUUID() + ext;
        Path target = rootDirectory.resolve(storedName).normalize();
        if (!target.startsWith(rootDirectory)) {
            throw new BadRequestException("Chemin invalide");
        }

        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Échec écriture média {}", storedName, e);
            throw new BadRequestException("Impossible d'enregistrer le fichier média");
        }

        log.info("[MEDIA] Stored {} as {} ({})", original, storedName, mediaType);
        return new StoredMedia(storedName, mediaType, contentType != null ? contentType : "application/octet-stream");
    }

    public Resource loadAsResource(String storedFileName) {
        try {
            if (storedFileName == null || storedFileName.contains("..") || storedFileName.contains("/") || storedFileName.contains("\\")) {
                throw new BadRequestException("Nom de fichier invalide");
            }
            Path p = rootDirectory.resolve(storedFileName).normalize();
            if (!p.startsWith(rootDirectory)) {
                throw new BadRequestException("Accès refusé");
            }
            if (!Files.exists(p)) {
                return null;
            }
            Resource resource = new UrlResource(p.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return null;
            }
            return resource;
        } catch (Exception e) {
            log.warn("Média introuvable: {}", storedFileName);
            return null;
        }
    }

    public String guessContentType(String storedFileName) {
        if (storedFileName == null) return "application/octet-stream";
        if (storedFileName.toLowerCase(Locale.ROOT).endsWith(".mp4")) return "video/mp4";
        if (storedFileName.toLowerCase(Locale.ROOT).endsWith(".mp3")) return "audio/mpeg";
        return "application/octet-stream";
    }

    public record StoredMedia(String storedFileName, SubcontractMediaType mediaType, String contentType) {}
}
