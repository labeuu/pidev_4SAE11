package tn.esprit.freelanciajob.Dto.projection;

/** Closed interface projection — maps directly to a GROUP BY status query. */
public interface StatusCountProjection {
    String getStatus();
    Long   getCount();
}
