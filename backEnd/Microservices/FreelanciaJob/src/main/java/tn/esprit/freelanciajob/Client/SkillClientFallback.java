package tn.esprit.freelanciajob.Client;

import org.springframework.stereotype.Component;
import tn.esprit.freelanciajob.Dto.Skills;

import java.util.Collections;
import java.util.List;

@Component
public class SkillClientFallback implements SkillClient {

    @Override
    public List<Skills> getSkillsByIds(List<Long> ids) {
        return Collections.emptyList();
    }

    @Override
    public List<Skills> getSkillsByUserId(Long userId) {
        return Collections.emptyList();
    }
}
