package tn.esprit.meeting.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tn.esprit.meeting.dto.ProjectDto;

import java.util.List;

@Component
@Slf4j
public class ProjectClientFallback implements ProjectClient {

    @Override
    public List<ProjectDto> getProjectsByClient(Long clientId) {
        log.warn("[MeetingService] ProjectClient fallback for clientId={}", clientId);
        return List.of();
    }

    @Override
    public List<ProjectDto> getProjectsByFreelancer(Long freelancerId) {
        log.warn("[MeetingService] ProjectClient fallback for freelancerId={}", freelancerId);
        return List.of();
    }
}
