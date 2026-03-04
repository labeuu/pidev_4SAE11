package tn.esprit.project.Client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tn.esprit.project.Dto.Skills;

import java.util.Collections;
import java.util.List;

/**
 * Fallback when the Portfolio microservice is unavailable.
 * Returns empty lists so recommendations and project enrichment degrade gracefully.
 */
@Slf4j
@Component
public class SkillClientFallback implements SkillClient {

    @Override
    public List<Skills> getSkillsByIds(List<Long> ids) {
        log.warn("SkillClient fallback: Portfolio unavailable for getSkillsByIds, returning empty");
        return ids == null || ids.isEmpty() ? List.of() : Collections.emptyList();
    }

    @Override
    public List<Skills> getSkillsByUserId(Long userId) {
        log.warn("SkillClient fallback: Portfolio unavailable for getSkillsByUserId(userId={}), returning empty",
                userId);
        return Collections.emptyList();
    }
}
