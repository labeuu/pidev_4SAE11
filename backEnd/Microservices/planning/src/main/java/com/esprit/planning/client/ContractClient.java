package com.esprit.planning.client;

import com.esprit.planning.dto.ContractDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "Contract", path = "/api/contracts", contextId = "contractPlanning")
public interface ContractClient {
    @GetMapping("/freelancer/{freelancerId}")
    List<ContractDto> getContractsByFreelancer(@PathVariable("freelancerId") Long freelancerId);
}
