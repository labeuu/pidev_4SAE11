package com.esprit.task.service;

import com.esprit.task.client.ProjectApplicationClient;
import com.esprit.task.dto.ProjectApplicationFeignDto;
import com.esprit.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskFreelancerProjectAccessService {

    private final TaskRepository taskRepository;
    private final ProjectApplicationClient projectApplicationClient;

    /**
     * Mirrors freelancer "projects for add" rules: has a task on the project as assignee,
     * or has an ACCEPTED application for that project.
     */
    public boolean canFreelancerUseProject(Long freelancerId, Long projectId) {
        if (freelancerId == null || projectId == null) {
            return false;
        }
        if (taskRepository.existsByProjectIdAndAssigneeId(projectId, freelancerId)) {
            return true;
        }
        try {
            List<ProjectApplicationFeignDto> apps = projectApplicationClient.getApplicationsByFreelance(freelancerId);
            if (apps == null || apps.isEmpty()) {
                return false;
            }
            for (ProjectApplicationFeignDto a : apps) {
                if (!isAcceptedStatus(a.getStatus())) {
                    continue;
                }
                if (a.getProject() == null || a.getProject().getId() == null) {
                    continue;
                }
                if (projectId.equals(a.getProject().getId())) {
                    return true;
                }
            }
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    private static boolean isAcceptedStatus(String status) {
        if (status == null) {
            return false;
        }
        String s = status.trim();
        return "ACCEPTED".equalsIgnoreCase(s);
    }
}
