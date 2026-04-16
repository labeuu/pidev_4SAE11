package org.example.subcontracting.controller;

import lombok.RequiredArgsConstructor;
import org.example.subcontracting.dto.response.MediaUploadResponse;
import org.example.subcontracting.service.SubcontractMediaStorageService;
import org.example.subcontracting.service.SubcontractMediaStorageService.StoredMedia;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/subcontracts/media")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SubcontractMediaController {

    private final SubcontractMediaStorageService storageService;

    @Value("${app.subcontract.media.download-base-url:http://localhost:8110}")
    private String downloadBaseUrl;

    /**
     * Upload MP4 (vidéo) ou MP3 (audio). Retourne l’URL publique à enregistrer sur la sous-traitance.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaUploadResponse> upload(
            @RequestParam Long mainFreelancerId,
            @RequestParam("file") MultipartFile file) {
        if (mainFreelancerId == null || mainFreelancerId <= 0) {
            return ResponseEntity.badRequest().build();
        }
        StoredMedia stored = storageService.save(file);
        String base = downloadBaseUrl.replaceAll("/$", "");
        String publicUrl = base + "/api/subcontracts/media/files/" + stored.storedFileName();
        return ResponseEntity.ok(MediaUploadResponse.builder()
                .mediaUrl(publicUrl)
                .mediaType(stored.mediaType().name())
                .build());
    }

    @GetMapping("/files/{fileName:.+}")
    public ResponseEntity<Resource> stream(@PathVariable String fileName) {
        Resource resource = storageService.loadAsResource(fileName);
        if (resource == null) {
            return ResponseEntity.notFound().build();
        }
        String ct = storageService.guessContentType(fileName);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(ct))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .body(resource);
    }
}
