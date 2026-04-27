package org.example.contract.service;

import org.example.contract.dto.ConflictStatsDto;
import org.example.contract.entity.Conflict;
import org.example.contract.entity.ConflictStatus;
import org.example.contract.entity.Contract;
import org.example.contract.repository.ConflictRepository;
import org.example.contract.repository.ContractRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConflictServiceTest {

    @Mock
    private ConflictRepository conflictRepository;
    @Mock
    private ContractRepository contractRepository;

    @InjectMocks
    private ConflictService conflictService;

    @Test
    void createConflictLinksContract() {
        Contract c = new Contract();
        c.setId(1L);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(c));
        Conflict input = new Conflict();
        when(conflictRepository.save(any(Conflict.class))).thenAnswer(inv -> inv.getArgument(0));
        Conflict out = conflictService.createConflict(1L, input);
        assertEquals(ConflictStatus.OPEN, out.getStatus());
        assertEquals(c, out.getContract());
    }

    @Test
    void createThrowsWhenContractMissing() {
        when(contractRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> conflictService.createConflict(1L, new Conflict()));
    }

    @Test
    void getConflictByIdThrows() {
        when(conflictRepository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> conflictService.getConflictById(2L));
    }

    @Test
    void updateResolvedSetsTimestamp() {
        Conflict cf = new Conflict();
        cf.setId(3L);
        when(conflictRepository.findById(3L)).thenReturn(Optional.of(cf));
        when(conflictRepository.save(any(Conflict.class))).thenAnswer(inv -> inv.getArgument(0));
        Conflict out = conflictService.updateConflictStatus(3L, ConflictStatus.RESOLVED);
        assertEquals(ConflictStatus.RESOLVED, out.getStatus());
        assertNotNull(out.getResolvedAt());
    }

    @Test
    void listByContractId() {
        when(conflictRepository.findByContractId(5L)).thenReturn(List.of());
        assertEquals(0, conflictService.getConflictsByContractId(5L).size());
    }

    @Test
    void statsAggregate() {
        when(conflictRepository.count()).thenReturn(9L);
        when(conflictRepository.countByStatus(ConflictStatus.OPEN)).thenReturn(1L);
        when(conflictRepository.countByStatus(ConflictStatus.IN_REVIEW)).thenReturn(2L);
        when(conflictRepository.countByStatus(ConflictStatus.RESOLVED)).thenReturn(3L);
        ConflictStatsDto dto = conflictService.getConflictStats();
        assertEquals(9, dto.getTotal());
        assertEquals(3, dto.getResolved());
    }
}
