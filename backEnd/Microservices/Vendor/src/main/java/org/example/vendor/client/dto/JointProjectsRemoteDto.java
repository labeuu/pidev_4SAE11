package org.example.vendor.client.dto;

import lombok.Data;

import java.util.List;

@Data
public class JointProjectsRemoteDto {
    private long sharedProjectCount;
    private List<JointProjectItemRemoteDto> projects;
}
