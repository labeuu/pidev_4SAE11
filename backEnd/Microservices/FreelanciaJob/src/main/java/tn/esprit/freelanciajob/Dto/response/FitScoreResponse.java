package tn.esprit.freelanciajob.Dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FitScoreResponse {
    /** Overall fit score, 0–100 */
    private int score;
    /** STRONG_MATCH | GOOD_MATCH | PARTIAL_MATCH | LOW_MATCH */
    private String tier;
    /** 1–2 sentence summary written by the LLM */
    private String summary;
    /** Skills the freelancer has that the job requires */
    private List<String> matchedSkills;
    /** Skills the job requires that the freelancer lacks */
    private List<String> missingSkills;
    /** Actionable tips to improve the application */
    private List<String> recommendations;
}
