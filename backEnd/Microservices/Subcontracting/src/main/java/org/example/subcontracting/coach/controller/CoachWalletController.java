package org.example.subcontracting.coach.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.subcontracting.coach.CoachWalletService;
import org.example.subcontracting.coach.dto.AdminCreditRequest;
import org.example.subcontracting.coach.dto.AdminDebitRequest;
import org.example.subcontracting.coach.dto.RechargeRequestDto;
import org.example.subcontracting.coach.dto.RemainingAnalysesResponse;
import org.example.subcontracting.coach.dto.TransactionResponse;
import org.example.subcontracting.coach.dto.WalletResponse;
import org.example.subcontracting.coach.entity.CoachFeatureCost;
import org.example.subcontracting.coach.entity.CoachWallet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/coach/wallet")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CoachWalletController {

    private final CoachWalletService coachWalletService;

    @GetMapping("/me")
    public ResponseEntity<WalletResponse> me(@RequestParam Long userId) {
        return ResponseEntity.ok(coachWalletService.buildWalletResponse(userId));
    }

    @GetMapping("/me/transactions")
    public ResponseEntity<Page<TransactionResponse>> myTransactions(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable p = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(coachWalletService.myTransactions(userId, p));
    }

    @GetMapping("/me/remaining-analyses")
    public ResponseEntity<Map<String, RemainingAnalysesResponse>> remaining(
            @RequestParam Long userId) {
        return ResponseEntity.ok(coachWalletService.remainingAnalyses(userId));
    }

    @PostMapping("/me/recharge-request")
    public ResponseEntity<Void> rechargeRequest(
            @RequestParam Long userId,
            @RequestBody(required = false) RechargeRequestDto dto) {
        coachWalletService.requestRecharge(userId, dto);
        return ResponseEntity.accepted().build();
    }

    // ── Admin ─────────────────────────────────────────────────────────────

    @GetMapping("/admin/all")
    public ResponseEntity<Page<CoachWallet>> adminAll(
            @RequestParam Long adminUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireAdmin(adminUserId);
        Pageable p = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(coachWalletService.adminListWallets(p));
    }

    @GetMapping("/admin/{userId}")
    public ResponseEntity<CoachWallet> adminWallet(
            @RequestParam Long adminUserId,
            @PathVariable Long userId) {
        requireAdmin(adminUserId);
        return ResponseEntity.ok(coachWalletService.adminGetWallet(userId));
    }

    @PostMapping("/admin/{userId}/credit")
    public ResponseEntity<Void> adminCredit(
            @RequestParam Long adminUserId,
            @PathVariable Long userId,
            @Valid @RequestBody AdminCreditRequest body) {
        requireAdmin(adminUserId);
        coachWalletService.creditAdmin(userId, body.getAmount(), body.getReason(), adminUserId, body.getAdminNote());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/admin/{userId}/debit")
    public ResponseEntity<Void> adminDebit(
            @RequestParam Long adminUserId,
            @PathVariable Long userId,
            @Valid @RequestBody AdminDebitRequest body) {
        requireAdmin(adminUserId);
        coachWalletService.debitAdmin(userId, body.getAmount(), body.getReason(), adminUserId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/admin/{userId}/transactions")
    public ResponseEntity<Page<TransactionResponse>> adminUserTx(
            @RequestParam Long adminUserId,
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        requireAdmin(adminUserId);
        Pageable p = PageRequest.of(page, Math.min(size, 200));
        return ResponseEntity.ok(coachWalletService.adminUserTransactions(userId, p));
    }

    @GetMapping("/admin/audit-log")
    public ResponseEntity<Page<TransactionResponse>> adminAudit(
            @RequestParam Long adminUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        requireAdmin(adminUserId);
        Pageable p = PageRequest.of(page, Math.min(size, 200));
        return ResponseEntity.ok(coachWalletService.adminAudit(p));
    }

    @PatchMapping("/admin/{userId}/block")
    public ResponseEntity<Void> adminBlock(
            @RequestParam Long adminUserId,
            @PathVariable Long userId,
            @RequestParam boolean blocked) {
        requireAdmin(adminUserId);
        coachWalletService.setBlocked(userId, blocked, adminUserId);
        return ResponseEntity.ok().build();
    }

    private static void requireAdmin(Long adminUserId) {
        if (adminUserId == null || adminUserId <= 0) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "adminUserId requis");
        }
    }
}
