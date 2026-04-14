package tn.esprit.freelanciajob.Specification;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import tn.esprit.freelanciajob.Dto.request.JobSearchRequest;
import tn.esprit.freelanciajob.Entity.Enums.ClientType;
import tn.esprit.freelanciajob.Entity.Enums.JobStatus;
import tn.esprit.freelanciajob.Entity.Enums.LocationType;
import tn.esprit.freelanciajob.Entity.Job;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Criteria API specification builder for dynamic Job filtering.
 * Each predicate is null-safe: a null/blank input returns null which
 * Specification.where() treats as a no-op (match all).
 */
public class JobSpecification {

    private JobSpecification() {}

    /**
     * Compose all active predicates into a single Specification.
     * Spring Data JPA 4.x rejects null in both where() and and(), so we
     * collect only non-null specs and reduce them.
     */
    @SuppressWarnings("unchecked")
    public static Specification<Job> build(JobSearchRequest req) {
        return Stream.<Specification<Job>>of(
                        hasKeyword(req.getKeyword()),
                        hasClientId(req.getClientId()),
                        hasStatus(req.getStatus()),
                        hasClientType(req.getClientType()),
                        hasLocationType(req.getLocationType()),
                        budgetRange(req.getBudgetMin(), req.getBudgetMax()),
                        hasCategory(req.getCategory()),
                        hasAnySkill(req.getSkillIds())
                )
                .filter(Objects::nonNull)
                .reduce(Specification::and)
                .orElse((root, query, cb) -> cb.conjunction());
    }

    /** Case-insensitive partial match on title OR description. */
    private static Specification<Job> hasKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) return null;
        return (root, query, cb) -> {
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            );
        };
    }

    /** Exact match on clientId — used for "My Jobs" (CLIENT view). */
    private static Specification<Job> hasClientId(Long clientId) {
        if (clientId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("clientId"), clientId);
    }

    /** Exact match on JobStatus enum. */
    private static Specification<Job> hasStatus(JobStatus status) {
        if (status == null) return null;
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    /** Exact match on ClientType enum. */
    private static Specification<Job> hasClientType(ClientType clientType) {
        if (clientType == null) return null;
        return (root, query, cb) -> cb.equal(root.get("clientType"), clientType);
    }

    /** Exact match on LocationType enum. */
    private static Specification<Job> hasLocationType(LocationType locationType) {
        if (locationType == null) return null;
        return (root, query, cb) -> cb.equal(root.get("locationType"), locationType);
    }

    /**
     * Overlapping budget range filter.
     * A job matches when its budget range overlaps [budgetMin, budgetMax]:
     *   job.budgetMax >= clientMin  AND  job.budgetMin <= clientMax
     */
    private static Specification<Job> budgetRange(BigDecimal budgetMin, BigDecimal budgetMax) {
        if (budgetMin == null && budgetMax == null) return null;
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (budgetMin != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("budgetMax"), budgetMin));
            }
            if (budgetMax != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("budgetMin"), budgetMax));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** Exact match on category string. */
    private static Specification<Job> hasCategory(String category) {
        if (category == null || category.isBlank()) return null;
        return (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    /**
     * Match jobs that contain ANY of the given skill IDs in requiredSkillIds.
     * Joins the element-collection table; requires query.distinct(true) to
     * avoid duplicates when a job matches multiple skills.
     */
    private static Specification<Job> hasAnySkill(List<Long> skillIds) {
        if (skillIds == null || skillIds.isEmpty()) return null;
        return (root, query, cb) -> {
            if (Long.class != query.getResultType()) {
                query.distinct(true);
            }
            Join<Object, Object> skillJoin = root.join("requiredSkillIds");
            return skillJoin.in(skillIds);
        };
    }
}
