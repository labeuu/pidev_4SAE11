package tn.esprit.meeting.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tn.esprit.meeting.dto.ContractDto;

import java.util.List;

@Component
@Slf4j
public class ContractClientFallback implements ContractClient {

    @Override
    public List<ContractDto> getContractsByClient(Long clientId) {
        log.warn("[MeetingService] ContractClient fallback for clientId={}", clientId);
        return List.of();
    }

    @Override
    public List<ContractDto> getContractsByFreelancer(Long freelancerId) {
        log.warn("[MeetingService] ContractClient fallback for freelancerId={}", freelancerId);
        return List.of();
    }
}
