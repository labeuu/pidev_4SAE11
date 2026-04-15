package tn.esprit.freelanciajob.Client;

import org.springframework.stereotype.Component;
import tn.esprit.freelanciajob.Dto.ExperienceDto;

import java.util.Collections;
import java.util.List;

@Component
public class ExperienceClientFallback implements ExperienceClient {

    @Override
    public List<ExperienceDto> getExperiencesByUserId(Long userId) {
        return Collections.emptyList();
    }
}
