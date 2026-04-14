package tn.esprit.freelanciajob.Dto.projection;

/** Closed interface projection for the native monthly-count query. */
public interface MonthlyJobProjection {
    String getMonth();  // format "YYYY-MM"
    Long   getCount();
}
