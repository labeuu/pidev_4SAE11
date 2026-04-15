package org.example.subcontracting.coach;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.subcontracting.coach.dto.*;
import org.example.subcontracting.coach.entity.CoachFeatureCost;
import org.example.subcontracting.coach.entity.CoachWallet;
import org.example.subcontracting.coach.entity.CoachWalletTransaction;
import org.example.subcontracting.coach.exception.AlreadyUsedFreeInsightException;
import org.example.subcontracting.coach.exception.InsufficientPointsException;
import org.example.subcontracting.coach.exception.WalletBlockedException;
import org.example.subcontracting.coach.repository.CoachFeatureCostRepository;
import org.example.subcontracting.coach.repository.CoachWalletRepository;
import org.example.subcontracting.coach.repository.CoachWalletTransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoachWalletService {

    private final CoachWalletRepository walletRepository;
    private final CoachWalletTransactionRepository transactionRepository;
    private final CoachFeatureCostRepository featureCostRepository;
    private final CoachWalletProperties properties;
    private final CoachNotificationDispatcher notificationDispatcher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public CoachWallet getOrCreateWallet(Long userId) {
        return walletRepository.findByUserId(userId).orElseGet(() -> createWalletWithWelcome(userId));
    }

    private CoachWallet createWalletWithWelcome(Long userId) {
        CoachWallet w = walletRepository.save(CoachWallet.builder()
                .userId(userId)
                .balance(0)
                .currency("COACH_POINTS")
                .blocked(false)
                .firstFreeUsed(false)
                .lowBalanceAlerted(false)
                .build());
        int bonus = properties.getWelcomeBonusPoints();
        if (bonus > 0) {
            applyCredit(w.getId(), bonus, WalletTransactionType.BONUS, "WELCOME_BONUS", null,
                    null, "SYSTEM", null);
            notificationDispatcher.walletBonusWelcome(userId, bonus);
        }
        return walletRepository.findById(w.getId()).orElseThrow();
    }

    /** Pas en readOnly : peut créer le wallet à la première visite. */
    @Transactional
    public WalletResponse buildWalletResponse(Long userId) {
        CoachWallet w = getOrCreateWallet(userId);
        return toWalletResponse(w);
    }

    @Transactional
    public Page<TransactionResponse> myTransactions(Long userId, Pageable pageable) {
        CoachWallet w = getOrCreateWallet(userId);
        return transactionRepository.findByWallet_IdOrderByCreatedAtDesc(w.getId(), pageable)
                .map(this::toTxResponse);
    }

    @Transactional
    public Map<String, RemainingAnalysesResponse> remainingAnalyses(Long userId) {
        return remainingAnalysesForWallet(getOrCreateWallet(userId));
    }

    private Map<String, RemainingAnalysesResponse> remainingAnalysesForWallet(CoachWallet w) {
        Map<String, RemainingAnalysesResponse> map = new LinkedHashMap<>();
        for (CoachFeatureCost fc : featureCostRepository.findAllByOrderByFeatureCodeAsc()) {
            if (!Boolean.TRUE.equals(fc.getActive())) continue;
            if (CoachFeatureCode.INITIAL_FREE.name().equals(fc.getFeatureCode())) continue;
            int cost = fc.getCostPoints();
            int rem = cost <= 0 ? 0 : w.getBalance() / cost;
            map.put(fc.getFeatureCode(), RemainingAnalysesResponse.builder()
                    .featureCode(fc.getFeatureCode())
                    .costPoints(cost)
                    .canAfford(w.getBalance() >= cost)
                    .remainingCount(rem)
                    .build());
        }
        return map;
    }

    @Transactional
    public DebitResultResponse debitForFeature(Long userId, CoachFeatureCode featureCode, String metadataJson) {
        CoachWallet w = getOrCreateWallet(userId);
        if (Boolean.TRUE.equals(w.getBlocked())) {
            throw new WalletBlockedException();
        }
        CoachFeatureCost costRow = featureCostRepository.findByFeatureCodeAndActiveIsTrue(featureCode.name())
                .orElseThrow(() -> new IllegalStateException("Coût inconnu pour " + featureCode));
        int cost = costRow.getCostPoints();
        if (w.getBalance() < cost) {
            throw new InsufficientPointsException(w.getBalance(), cost);
        }
        int before = w.getBalance();
        int after = before - cost;
        w.setBalance(after);
        walletRepository.save(w);
        persistTx(w, WalletTransactionType.DEBIT, cost, before, after,
                featureCode.name(), featureCode, userId, "USER", metadataJson);
        postDebitRules(w, userId);
        w = walletRepository.findById(w.getId()).orElseThrow();
        return DebitResultResponse.builder()
                .success(true)
                .balanceBefore(before)
                .balanceAfter(w.getBalance())
                .pointsSpent(cost)
                .remainingAnalyses(remainingAnalyses(userId))
                .lowBalance(w.getBalance() <= properties.getLowBalanceThreshold())
                .blocked(Boolean.TRUE.equals(w.getBlocked()))
                .build();
    }

    private void postDebitRules(CoachWallet w, Long userId) {
        CoachWallet fresh = walletRepository.findById(w.getId()).orElseThrow();
        int bal = fresh.getBalance();
        if (bal <= properties.getLowBalanceThreshold() && !Boolean.TRUE.equals(fresh.getLowBalanceAlerted())) {
            fresh.setLowBalanceAlerted(true);
            walletRepository.save(fresh);
            notificationDispatcher.walletLowBalance(userId, bal);
        }
        if (bal <= properties.getCriticalBalanceThreshold()) {
            fresh.setBlocked(true);
            walletRepository.save(fresh);
            notificationDispatcher.walletEmptyBlocked(userId);
        }
    }

    @Transactional
    public void creditAdmin(Long targetUserId, int amount, String reason, Long adminId, String adminNote) {
        CoachWallet w = getOrCreateWallet(targetUserId);
        applyCredit(w.getId(), amount, WalletTransactionType.CREDIT, reason, null,
                adminId, "ADMIN", adminNote);
        CoachWallet updated = walletRepository.findByUserId(targetUserId).orElseThrow();
        notificationDispatcher.walletCredited(targetUserId, amount, updated.getBalance());
    }

    @Transactional
    public void debitAdmin(Long targetUserId, int amount, String reason, Long adminId) {
        CoachWallet w = getOrCreateWallet(targetUserId);
        if (w.getBalance() < amount) {
            throw new InsufficientPointsException(w.getBalance(), amount);
        }
        int before = w.getBalance();
        int after = before - amount;
        w.setBalance(after);
        walletRepository.save(w);
        persistTx(w, WalletTransactionType.ADMIN_ADJUSTMENT, amount, before, after,
                reason, null, adminId, "ADMIN", null);
        postDebitRules(w, targetUserId);
    }

    /**
     * Consomme l’analyse gratuite : vérifie le flag, enregistre la transaction INITIAL_FREE.
     */
    @Transactional
    public void saveInitialFreeConsumption(Long userId, String metadataJson) {
        CoachWallet w = getOrCreateWallet(userId);
        if (Boolean.TRUE.equals(w.getFirstFreeUsed())) {
            throw new AlreadyUsedFreeInsightException();
        }
        int bal = w.getBalance();
        w.setFirstFreeUsed(true);
        walletRepository.save(w);
        persistTx(w, WalletTransactionType.INITIAL_FREE, 0, bal, bal,
                "INITIAL_FREE_COACHING", CoachFeatureCode.INITIAL_FREE, userId, "USER", metadataJson);
    }

    @Transactional
    public void setBlocked(Long userId, boolean blocked, Long adminId) {
        CoachWallet w = getOrCreateWallet(userId);
        boolean wasBlocked = Boolean.TRUE.equals(w.getBlocked());
        w.setBlocked(blocked);
        walletRepository.save(w);
        persistTx(w, WalletTransactionType.ADMIN_ADJUSTMENT, 0, w.getBalance(), w.getBalance(),
                blocked ? "BLOCK" : "UNBLOCK", null, adminId, "ADMIN", null);
        if (wasBlocked && !blocked) {
            notificationDispatcher.walletUnblocked(userId, w.getBalance());
        }
    }

    @Transactional(readOnly = true)
    public Page<CoachWallet> adminListWallets(Pageable pageable) {
        return walletRepository.findAllByOrderByUpdatedAtDesc(pageable);
    }

    @Transactional
    public CoachWallet adminGetWallet(Long userId) {
        return getOrCreateWallet(userId);
    }

    @Transactional
    public Page<TransactionResponse> adminUserTransactions(Long userId, Pageable pageable) {
        return myTransactions(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> adminAudit(Pageable pageable) {
        return transactionRepository.findByPerformedByRoleIgnoreCaseOrderByCreatedAtDesc("ADMIN", pageable)
                .map(this::toTxResponse);
    }

    @Transactional
    public CoachFeatureCost updateFeatureCost(String code, int newCost, Long adminId) {
        CoachFeatureCost fc = featureCostRepository.findByFeatureCodeAndActiveIsTrue(code)
                .orElseGet(() -> featureCostRepository.findAll().stream()
                        .filter(c -> code.equalsIgnoreCase(c.getFeatureCode()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Feature inconnue: " + code)));
        fc.setCostPoints(newCost);
        fc.setUpdatedBy(adminId);
        fc.setUpdatedAt(java.time.Instant.now());
        return featureCostRepository.save(fc);
    }

    @Transactional(readOnly = true)
    public List<CoachFeatureCost> listFeatureCosts() {
        return featureCostRepository.findAllByOrderByFeatureCodeAsc();
    }

    @Transactional
    public void requestRecharge(Long userId, RechargeRequestDto dto) {
        notificationDispatcher.rechargeRequest(userId,
                dto != null ? dto.getSuggestedPoints() : null,
                dto != null ? dto.getMessage() : null);
    }

    /** Cron : rappels solde faible non flaggés */
    @Transactional
    public int scanLowBalanceWallets() {
        List<CoachWallet> list = walletRepository.findLowBalanceNotAlerted(properties.getLowBalanceThreshold());
        int n = 0;
        for (CoachWallet w : list) {
            notificationDispatcher.walletLowBalance(w.getUserId(), w.getBalance());
            w.setLowBalanceAlerted(true);
            walletRepository.save(w);
            n++;
        }
        return n;
    }

    private void applyCredit(Long walletId, int amount, WalletTransactionType type, String reason,
                             CoachFeatureCode feature, Long performerId, String performerRole,
                             String note) {
        CoachWallet w = walletRepository.findById(walletId).orElseThrow();
        applyCreditInternal(w, amount, type, reason, feature, performerId, performerRole, note);
    }

    private void applyCreditInternal(CoachWallet w, int amount, WalletTransactionType type, String reason,
                                     CoachFeatureCode feature, Long performerId, String performerRole,
                                     String note) {
        boolean wasBlocked = Boolean.TRUE.equals(w.getBlocked());
        int before = w.getBalance();
        int after = before + amount;
        w.setBalance(after);
        if (after > properties.getLowBalanceThreshold()) {
            w.setLowBalanceAlerted(false);
        }
        if (wasBlocked && after > properties.getCriticalBalanceThreshold()) {
            w.setBlocked(false);
        }
        walletRepository.save(w);
        if (wasBlocked && !Boolean.TRUE.equals(w.getBlocked())) {
            notificationDispatcher.walletUnblocked(w.getUserId(), after);
        }
        persistTx(w, type, amount, before, after, reason, feature, performerId, performerRole, note);
    }

    private void persistTx(CoachWallet w, WalletTransactionType type, int amount,
                           int balanceBefore, int balanceAfter, String reason,
                           CoachFeatureCode feature, Long performerId, String performerRole, String metadata) {
        String metaJson = null;
        if (metadata != null && !metadata.isBlank()) {
            try {
                if (metadata.startsWith("{")) {
                    metaJson = metadata;
                } else {
                    metaJson = objectMapper.writeValueAsString(Map.of("note", metadata));
                }
            } catch (JsonProcessingException e) {
                metaJson = null;
            }
        }
        CoachWalletTransaction tx = CoachWalletTransaction.builder()
                .wallet(w)
                .type(type)
                .amount(amount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .reason(reason)
                .featureUsed(feature)
                .performedBy(performerId)
                .performedByRole(performerRole)
                .metadataJson(metaJson)
                .build();
        transactionRepository.save(tx);
    }

    private WalletResponse toWalletResponse(CoachWallet w) {
        return WalletResponse.builder()
                .userId(w.getUserId())
                .balance(w.getBalance())
                .currency(w.getCurrency())
                .blocked(w.getBlocked())
                .firstFreeUsed(w.getFirstFreeUsed())
                .remainingAnalysesByFeature(remainingAnalysesForWallet(w))
                .build();
    }

    private TransactionResponse toTxResponse(CoachWalletTransaction t) {
        return TransactionResponse.builder()
                .id(t.getId())
                .type(t.getType())
                .amount(t.getAmount())
                .balanceBefore(t.getBalanceBefore())
                .balanceAfter(t.getBalanceAfter())
                .reason(t.getReason())
                .featureUsed(t.getFeatureUsed())
                .performedByRole(t.getPerformedByRole())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
