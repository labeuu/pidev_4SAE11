package com.esprit.task.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractDto {
    private Long id;
    private Long projectId;
    private Long freelancerId;
    private Long clientId;
    private String status;
}
