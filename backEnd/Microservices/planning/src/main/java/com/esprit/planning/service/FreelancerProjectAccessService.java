package com.esprit.planning.service;

import com.esprit.planning.client.ContractClient;
import com.esprit.planning.client.ProjectApplicationClient;
import com.esprit.planning.dto.ContractDto;
import com.esprit.planning.dto.ProjectApplicationFeignDto;
import com.esprit.planning.repository.ProgressUpdateRepository;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FreelancerProjectAccessService {
    private final ProjectApplicationClient projectApplicationClient;
    private final ContractClient contractClient;
    private final ProgressUpdateRepository progressUpdateRepository;

    public FreelancerProjectAccessService(ProjectApplicationClient projectApplicationClient,
                                          ContractClient contractClient,
                                          ProgressUpdateRepository progressUpdateRepository) {
        this.projectApplicationClient = projectApplicationClient;
        this.contractClient = contractClient;
        this.progressUpdateRepository = progressUpdateRepository;
    }

    public Set<Long> getAccessibleProjectIdsForFreelancer(Long freelancerId) {
        Set<Long> projectIds = new HashSet<>();
        if (freelancerId == null) {
            return projectIds;
        }

        try {
            List<ProjectApplicationFeignDto> applications = projectApplicationClient.getApplicationsByFreelance(freelancerId);
            if (applications != null) {
                applications.stream()
                        .filter(a -> isAcceptedStatus(a.getStatus()))
                        .map(ProjectApplicationFeignDto::getProject)
                        .filter(p -> p != null && p.getId() != null)
                        .map(ProjectApplicationFeignDto.NestedProject::getId)
                        .forEach(projectIds::add);
            }
        } catch (Exception ignored) {
            // Keep best-effort union behavior.
        }

        try {
            List<ContractDto> contracts = contractClient.getContractsByFreelancer(freelancerId);
            if (contracts != null) {
                Set<Long> activeContractIds = contracts.stream()
                        .filter(c -> c != null && c.getId() != null && isAcceptedContractStatus(c.getStatus()))
                        .map(ContractDto::getId)
                        .collect(Collectors.toSet());
                if (!activeContractIds.isEmpty()) {
                    projectIds.addAll(progressUpdateRepository.findDistinctProjectIdsByContractIdIn(activeContractIds));
                }
            }
        } catch (Exception ignored) {
            // Keep best-effort union behavior.
        }

        return projectIds;
    }

    public boolean canFreelancerAccessProject(Long freelancerId, Long projectId) {
        return projectId != null && getAccessibleProjectIdsForFreelancer(freelancerId).contains(projectId);
    }

    private static boolean isAcceptedStatus(String status) {
        return status != null && "ACCEPTED".equalsIgnoreCase(status.trim());
    }

    private static boolean isAcceptedContractStatus(String status) {
        if (status == null) {
            return false;
        }
        String s = status.trim();
        return "ACTIVE".equalsIgnoreCase(s)
                || "COMPLETED".equalsIgnoreCase(s)
                || "IN_CONFLICT".equalsIgnoreCase(s);
    }
}
