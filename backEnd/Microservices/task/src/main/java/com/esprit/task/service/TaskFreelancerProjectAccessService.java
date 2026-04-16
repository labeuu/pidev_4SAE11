package com.esprit.task.service;

import com.esprit.task.client.ProjectApplicationClient;
import com.esprit.task.client.ContractClient;
import com.esprit.task.dto.ContractDto;
import com.esprit.task.dto.ProjectApplicationFeignDto;
import com.esprit.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TaskFreelancerProjectAccessService {

    private final ProjectApplicationClient projectApplicationClient;
    private final ContractClient contractClient;
    private final TaskRepository taskRepository;

    /**
     * Mirrors freelancer "projects for add" rules: has a task on the project as assignee,
     * or has an ACCEPTED application for that project.
     */
    public boolean canFreelancerUseProject(Long freelancerId, Long projectId) {
        if (freelancerId == null || projectId == null) {
            return false;
        }
        return getAccessibleProjectIdsForFreelancer(freelancerId).contains(projectId);
    }

    /**
     * Resolves projects visible to a freelancer: assigned tasks on the project,
     * accepted project applications, and active-style contracts.
     */
    public Set<Long> getAccessibleProjectIdsForFreelancer(Long freelancerId) {
        Set<Long> projectIds = new HashSet<>();
        if (freelancerId == null) {
            return projectIds;
        }
        try {
            List<Long> assignedProjectIds = taskRepository.findDistinctProjectIdsByAssigneeId(freelancerId);
            if (assignedProjectIds != null) {
                projectIds.addAll(assignedProjectIds);
            }
        } catch (Exception ignored) {
            // same tolerance as remote sources — Feign/DB issues should not block other access paths
        }
        try {
            List<ProjectApplicationFeignDto> apps = projectApplicationClient.getApplicationsByFreelance(freelancerId);
            if (apps != null) {
                for (ProjectApplicationFeignDto a : apps) {
                    if (!isAcceptedStatus(a.getStatus())) {
                        continue;
                    }
                    if (a.getProject() == null || a.getProject().getId() == null) {
                        continue;
                    }
                    projectIds.add(a.getProject().getId());
                }
            }
        } catch (Exception ignored) {
            // ignored on purpose; union continues with other sources
        }
        try {
            List<ContractDto> contracts = contractClient.getContractsByFreelancer(freelancerId);
            if (contracts != null) {
                for (ContractDto c : contracts) {
                    if (c == null || c.getProjectId() == null || !isAcceptedContractStatus(c.getStatus())) {
                        continue;
                    }
                    projectIds.add(c.getProjectId());
                }
            }
        } catch (Exception ignored) {
            // ignored on purpose; return available subset
        }
        return projectIds;
    }

    // Checks whether accepted status.
    private static boolean isAcceptedStatus(String status) {
        if (status == null) {
            return false;
        }
        String s = status.trim();
        return "ACCEPTED".equalsIgnoreCase(s);
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
