package tn.esprit.freelanciajob.Dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AttachmentResponse {
    private Long          id;
    private Long          jobApplicationId;
    private String        fileName;
    private String        fileType;
    private String        fileUrl;      // relative URL; frontend prepends gateway base
    private Long          fileSize;     // bytes
    private LocalDateTime uploadedAt;
}
