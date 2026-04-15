package com.esprit.task.client;

import com.esprit.task.dto.ContractDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "contract", url = "${contract.service.url:http://localhost:8083}", path = "/api/contracts")
public interface ContractClient {

    @GetMapping("/{id}")
    ContractDto getContractById(@PathVariable("id") Long id);

    @GetMapping("/freelancer/{freelancerId}")
    List<ContractDto> getContractsByFreelancer(@PathVariable("freelancerId") Long freelancerId);
}
