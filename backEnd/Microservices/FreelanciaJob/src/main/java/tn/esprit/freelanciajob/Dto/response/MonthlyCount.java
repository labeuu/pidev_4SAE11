package tn.esprit.freelanciajob.Dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyCount {
    private String month; // "YYYY-MM"
    private long   count;
}
