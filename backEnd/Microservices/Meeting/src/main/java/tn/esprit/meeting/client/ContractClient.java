package tn.esprit.meeting.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import tn.esprit.meeting.dto.ContractDto;

import java.util.List;

@FeignClient(name = "contract", path = "/api/contracts", fallback = ContractClientFallback.class)
public interface ContractClient {

    @GetMapping("/client/{clientId}")
    List<ContractDto> getContractsByClient(@PathVariable("clientId") Long clientId);

    @GetMapping("/freelancer/{freelancerId}")
    List<ContractDto> getContractsByFreelancer(@PathVariable("freelancerId") Long freelancerId);
}
