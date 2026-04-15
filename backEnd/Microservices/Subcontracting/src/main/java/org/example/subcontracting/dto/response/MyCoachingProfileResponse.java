package org.example.subcontracting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyCoachingProfileResponse {
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> patterns;
    private List<String> personalizedTips;
    private Integer progressScore;
}

