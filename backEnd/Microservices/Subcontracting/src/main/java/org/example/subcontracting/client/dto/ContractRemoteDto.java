package org.example.subcontracting.client.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ContractRemoteDto {
    private Long id;
    private BigDecimal amount;
    private String title;
}
