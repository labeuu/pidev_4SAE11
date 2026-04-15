package tn.esprit.freelanciajob.Dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import tn.esprit.freelanciajob.Entity.Enums.ClientType;
import tn.esprit.freelanciajob.Entity.Enums.JobStatus;
import tn.esprit.freelanciajob.Entity.Enums.LocationType;

import java.math.BigDecimal;
import java.util.List;

/**
 * Captures all dynamic filter criteria sent from the frontend, plus
 * pagination/sort parameters so results are always server-paged.
 */
@Data
public class JobSearchRequest {

    // ── Text search ──────────────────────────────────────────────────────────
    /** Partial, case-insensitive match on title or description. */
    private String keyword;

    // ── Ownership (Client "My Jobs" view) ────────────────────────────────────
    /** When present, restrict results to jobs posted by this client. */
    private Long clientId;

    // ── Enum filters ─────────────────────────────────────────────────────────
    private JobStatus status;
    private ClientType clientType;
    private LocationType locationType;

    // ── Category ─────────────────────────────────────────────────────────────
    private String category;

    // ── Budget range ─────────────────────────────────────────────────────────
    @Positive(message = "budgetMin must be positive")
    private BigDecimal budgetMin;

    @Positive(message = "budgetMax must be positive")
    private BigDecimal budgetMax;

    // ── Skills ───────────────────────────────────────────────────────────────
    /** Match jobs that require ANY of these skill IDs. */
    private List<Long> skillIds;

    // ── Pagination ───────────────────────────────────────────────────────────
    @Min(0)
    private int page = 0;

    @Min(1)
    private int size = 9;

    /** Field name on Job entity to sort by (e.g. "createdAt", "budgetMax"). */
    private String sortBy = "createdAt";

    /** "asc" or "desc". */
    private String sortDir = "desc";
}
