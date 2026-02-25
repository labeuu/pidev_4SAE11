package org.example.offer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Évolution mensuelle des offres (calcul backend, GROUP BY). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyEvolutionResponse {
    private int year;
    private List<MonthCount> months;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthCount {
        private int month;
        private long count;
    }
}
