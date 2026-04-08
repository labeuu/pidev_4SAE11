package org.example.vendor.client.dto;

import lombok.Data;

import java.util.List;

@Data
public class SkillRemoteDto {
    private Long id;
    private String name;
    private List<String> domains;
    private String description;
    private Long userId;
}
