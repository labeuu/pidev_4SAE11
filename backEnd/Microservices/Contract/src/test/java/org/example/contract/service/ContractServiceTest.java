package org.example.contract.service;

import org.example.contract.dto.ContractStatsDto;
import org.example.contract.entity.Contract;
import org.example.contract.entity.ContractStatus;
import org.example.contract.repository.ContractRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContractServiceTest {

    @Mock
    private ContractRepository contractRepository;

    @InjectMocks
    private ContractService contractService;

    private Contract existing;

    @BeforeEach
    void setUp() {
        existing = new Contract();
        existing.setId(1L);
        existing.setTitle("T");
        existing.setTerms("terms");
        existing.setAmount(BigDecimal.TEN);
        existing.setStartDate(LocalDate.now());
        existing.setEndDate(LocalDate.now().plusDays(1));
        existing.setStatus(ContractStatus.DRAFT);
    }

    @Test
    void createContractSetsDraftAndSaves() {
        Contract input = new Contract();
        when(contractRepository.save(any(Contract.class))).thenAnswer(inv -> inv.getArgument(0));
        Contract out = contractService.createContract(input);
        assertEquals(ContractStatus.DRAFT, out.getStatus());
        verify(contractRepository).save(input);
    }

    @Test
    void getAllDelegates() {
        when(contractRepository.findAll()).thenReturn(List.of(existing));
        assertEquals(1, contractService.getAllContracts().size());
    }

    @Test
    void getById() {
        when(contractRepository.findById(1L)).thenReturn(Optional.of(existing));
        assertEquals(1L, contractService.getContractById(1L).orElseThrow().getId());
    }

    @Test
    void updateContractMapsFieldsAndSaves() {
        Contract patch = new Contract();
        patch.setTitle("NewTitle");
        patch.setTerms("NewTerms");
        patch.setAmount(BigDecimal.ONE);
        patch.setStartDate(LocalDate.of(2025, 1, 1));
        patch.setEndDate(LocalDate.of(2025, 2, 1));
        patch.setStatus(ContractStatus.ACTIVE);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(contractRepository.save(any(Contract.class))).thenAnswer(inv -> inv.getArgument(0));
        Contract saved = contractService.updateContract(1L, patch);
        assertEquals("NewTitle", saved.getTitle());
        assertEquals(ContractStatus.ACTIVE, saved.getStatus());
        assertNotNull(saved.getSignedAt());
    }

    @Test
    void updateContractThrowsWhenMissing() {
        when(contractRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> contractService.updateContract(99L, new Contract()));
    }

    @Test
    void deleteCallsRepo() {
        contractService.deleteContract(3L);
        verify(contractRepository).deleteById(3L);
    }

    @Test
    void listByClientAndFreelancer() {
        when(contractRepository.findByClientId(2L)).thenReturn(List.of());
        when(contractRepository.findByFreelancerId(3L)).thenReturn(List.of());
        assertEquals(0, contractService.getContractsByClientId(2L).size());
        assertEquals(0, contractService.getContractsByFreelancerId(3L).size());
    }

    @Test
    void signFreelancerCompletesActivationWhenClientAlreadySigned() {
        Contract c = new Contract();
        c.setId(1L);
        c.setClientSignatureUrl("sig-client");
        when(contractRepository.findById(1L)).thenReturn(Optional.of(c));
        when(contractRepository.save(any(Contract.class))).thenAnswer(inv -> inv.getArgument(0));
        Contract done = contractService.signContract(1L, "FREELANCER", "sig-fl");
        assertEquals(ContractStatus.ACTIVE, done.getStatus());
        assertNotNull(done.getSignedAt());
    }

    @Test
    void signClientStoresSignatureOnly() {
        when(contractRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(contractRepository.save(any(Contract.class))).thenAnswer(inv -> inv.getArgument(0));
        Contract out = contractService.signContract(1L, "CLIENT", "sig-c");
        assertEquals("sig-c", out.getClientSignatureUrl());
        assertEquals(ContractStatus.DRAFT, out.getStatus());
    }

    @Test
    void signThrowsWhenContractMissing() {
        when(contractRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> contractService.signContract(1L, "CLIENT", "x"));
    }

    @Test
    void updateStatus() {
        when(contractRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(contractRepository.save(any(Contract.class))).thenAnswer(inv -> inv.getArgument(0));
        Contract c = contractService.updateStatus(1L, ContractStatus.COMPLETED);
        assertEquals(ContractStatus.COMPLETED, c.getStatus());
    }

    @Test
    void getContractStatsAggregates() {
        when(contractRepository.count()).thenReturn(10L);
        when(contractRepository.countByStatus(ContractStatus.DRAFT)).thenReturn(1L);
        when(contractRepository.countByStatus(ContractStatus.PENDING_SIGNATURE)).thenReturn(2L);
        when(contractRepository.countByStatus(ContractStatus.ACTIVE)).thenReturn(3L);
        when(contractRepository.countByStatus(ContractStatus.COMPLETED)).thenReturn(4L);
        when(contractRepository.countByStatus(ContractStatus.CANCELLED)).thenReturn(5L);
        when(contractRepository.countByStatus(ContractStatus.IN_CONFLICT)).thenReturn(6L);
        ContractStatsDto dto = contractService.getContractStats();
        assertEquals(10, dto.getTotal());
        assertEquals(6, dto.getInConflict());
    }
}
