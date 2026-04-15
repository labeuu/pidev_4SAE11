package org.example.subcontracting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaUploadResponse {

    /** URL absolue ou chemin utilisable dans l’e-mail et le front */
    private String mediaUrl;

    private String mediaType;
}
