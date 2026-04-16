package org.example.subcontracting.client;

import org.example.subcontracting.client.dto.ContractRemoteDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "Contract", path = "/api/contracts")
public interface ContractFeignClient {

    @GetMapping("/{id}")
    ContractRemoteDto getContractById(@PathVariable("id") Long id);
}
