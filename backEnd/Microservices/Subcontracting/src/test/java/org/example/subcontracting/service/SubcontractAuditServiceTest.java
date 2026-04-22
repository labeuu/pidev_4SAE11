package org.example.subcontracting.service;

import org.example.subcontracting.client.UserFeignClient;
import org.example.subcontracting.client.dto.UserRemoteDto;
import org.example.subcontracting.dto.response.FreelancerHistoryResponse;
import org.example.subcontracting.entity.Subcontract;
import org.example.subcontracting.entity.SubcontractAudit;
import org.example.subcontracting.repository.SubcontractAuditRepository;
import org.example.subcontracting.repository.SubcontractRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class SubcontractAuditServiceTest {

    @Mock
    private SubcontractAuditRepository auditRepo;
    @Mock
    private SubcontractRepository subcontractRepo;
    @Mock
    private UserFeignClient userClient;

    @InjectMocks
    private SubcontractAuditService subcontractAuditService;

    @Test
    void record_savesAuditEntry() {
        subcontractAuditService.record(10L, 20L, "CREATED", null, "DRAFT", "created", null, null);

        ArgumentCaptor<SubcontractAudit> captor = ArgumentCaptor.forClass(SubcontractAudit.class);
        verify(auditRepo).save(captor.capture());
        SubcontractAudit saved = captor.getValue();
        assertThat(saved.getSubcontractId()).isEqualTo(10L);
        assertThat(saved.getActorUserId()).isEqualTo(20L);
        assertThat(saved.getAction()).isEqualTo("CREATED");
        assertThat(saved.getToStatus()).isEqualTo("DRAFT");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void getFreelancerHistory_buildsAggregatesAndTimeline() {
        SubcontractAudit a1 = SubcontractAudit.builder()
                .id(1L)
                .subcontractId(100L)
                .actorUserId(7L)
                .action("CREATED")
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();
        SubcontractAudit a2 = SubcontractAudit.builder()
                .id(2L)
                .subcontractId(100L)
                .actorUserId(8L)
                .action("ACCEPTED")
                .createdAt(LocalDateTime.now())
                .build();
        when(auditRepo.findAllByFreelancerInvolved(7L)).thenReturn(List.of(a2, a1));
        when(subcontractRepo.countByMainFreelancerId(7L)).thenReturn(3L);
        when(subcontractRepo.countBySubcontractorId(7L)).thenReturn(2L);

        Subcontract subcontract = new Subcontract();
        subcontract.setId(100L);
        subcontract.setTitle("Refactor module");
        when(subcontractRepo.findById(100L)).thenReturn(Optional.of(subcontract));
        UserRemoteDto u1 = new UserRemoteDto();
        u1.setId(7L);
        u1.setFirstName("Ali");
        u1.setLastName("Ben");
        u1.setEmail("ali@demo.tn");
        u1.setRole("FREELANCER");
        UserRemoteDto u2 = new UserRemoteDto();
        u2.setId(8L);
        u2.setFirstName("Nour");
        u2.setLastName("Trabelsi");
        u2.setEmail("nour@demo.tn");
        u2.setRole("CLIENT");
        when(userClient.getUserById(7L)).thenReturn(u1);
        when(userClient.getUserById(8L)).thenReturn(u2);

        FreelancerHistoryResponse history = subcontractAuditService.getFreelancerHistory(7L);

        assertThat(history.getUserId()).isEqualTo(7L);
        assertThat(history.getUserName()).isEqualTo("Ali Ben");
        assertThat(history.getTotalEvents()).isEqualTo(2);
        assertThat(history.getAsMainFreelancer()).isEqualTo(3L);
        assertThat(history.getAsSubcontractor()).isEqualTo(2L);
        assertThat(history.getEventsByAction()).containsEntry("CREATED", 1L).containsEntry("ACCEPTED", 1L);
        assertThat(history.getTimeline()).hasSize(2);
        assertThat(history.getTimeline().get(0).getSubcontractTitle()).isEqualTo("Refactor module");
    }
}
