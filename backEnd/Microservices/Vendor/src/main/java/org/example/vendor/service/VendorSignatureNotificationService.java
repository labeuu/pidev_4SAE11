package org.example.vendor.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vendor.client.NotificationFeignClient;
import org.example.vendor.dto.notification.NotificationRequestDto;
import org.example.vendor.entity.VendorApproval;
import org.example.vendor.entity.VendorApprovalStatus;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Notifications proactives (microservice Notification) :
 * — signature requise / peer signed / both signed
 * — chaque changement de statut (approve, reject, suspend, expire, renew, resubmit)
 * — rappels avant expiration (J-30)
 * — révocation d'urgence (suspension bloquante)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VendorSignatureNotificationService {

    public static final String TYPE_VENDOR_SIGNATURE = "VENDOR_AGREEMENT_SIGNATURE";
    public static final String TYPE_VENDOR_PEER_SIGNED = "VENDOR_AGREEMENT_PEER_SIGNED";
    public static final String TYPE_VENDOR_BOTH_SIGNED = "VENDOR_AGREEMENT_BOTH_SIGNED";
    public static final String TYPE_VENDOR_EXPIRY_APPROACHING = "VENDOR_AGREEMENT_EXPIRY_APPROACHING";
    public static final String TYPE_VENDOR_STATUS_CHANGED = "VENDOR_AGREEMENT_STATUS_CHANGED";
    public static final String TYPE_VENDOR_EMERGENCY_REVOKE = "VENDOR_AGREEMENT_EMERGENCY_REVOKE";

    private final NotificationFeignClient notificationClient;

    // ── Signature notifications ───────────────────────────────

    public void notifySignatureRequired(VendorApproval va, String reason) {
        Map<String, String> data = baseData(va, TYPE_VENDOR_SIGNATURE);
        String extra = reason != null && !reason.isBlank() ? " " + reason : "";

        notify(
                String.valueOf(va.getOrganizationId()),
                "Signature électronique requise (client)",
                "Un agrément fournisseur #" + va.getId() + " vous concerne en tant qu'organisation."
                        + " Merci de le signer dans l'application (section Mes fournisseurs agréés)."
                        + extra,
                data,
                TYPE_VENDOR_SIGNATURE
        );
        notify(
                String.valueOf(va.getFreelancerId()),
                "Signature électronique requise (freelancer)",
                "L'administration a créé un agrément #" + va.getId() + " vous concernant."
                        + " Signez-le électroniquement dans Mes agréments fournisseur."
                        + extra,
                data,
                TYPE_VENDOR_SIGNATURE
        );
    }

    public void notifyAfterClientSigned(VendorApproval va) {
        if (va.getFreelancerSignedAt() != null) {
            return;
        }
        Map<String, String> data = baseData(va, TYPE_VENDOR_PEER_SIGNED);
        data.put("signedBy", "CLIENT");
        notify(
                String.valueOf(va.getFreelancerId()),
                "Agrément #" + va.getId() + " — le client a signé",
                "L'organisation a signé l'agrément. Merci de compléter votre signature électronique"
                        + " (Mes agréments fournisseur) si ce n'est pas déjà fait.",
                data,
                TYPE_VENDOR_PEER_SIGNED
        );
    }

    public void notifyAfterFreelancerSigned(VendorApproval va) {
        if (va.getClientSignedAt() != null) {
            return;
        }
        Map<String, String> data = baseData(va, TYPE_VENDOR_PEER_SIGNED);
        data.put("signedBy", "FREELANCER");
        notify(
                String.valueOf(va.getOrganizationId()),
                "Agrément #" + va.getId() + " — le freelancer a signé",
                "Le freelancer a signé l'agrément. Merci de compléter votre signature électronique"
                        + " (Mes fournisseurs agréés) si ce n'est pas déjà fait.",
                data,
                TYPE_VENDOR_PEER_SIGNED
        );
    }

    public void notifyBothPartiesFullySigned(VendorApproval va) {
        Map<String, String> data = baseData(va, TYPE_VENDOR_BOTH_SIGNED);
        String body = "L'agrément #" + va.getId() + " est signé par les deux parties."
                + " L'administrateur peut maintenant l'approuver.";
        notify(String.valueOf(va.getOrganizationId()),
                "Agrément #" + va.getId() + " — signatures complètes",
                body, data, TYPE_VENDOR_BOTH_SIGNED);
        notify(String.valueOf(va.getFreelancerId()),
                "Agrément #" + va.getId() + " — signatures complètes",
                body, data, TYPE_VENDOR_BOTH_SIGNED);
    }

    // ── Status change notifications (proactive) ──────────────

    /**
     * Generic notification sent to both parties on every status transition.
     */
    public void notifyStatusChanged(VendorApproval va, VendorApprovalStatus from, VendorApprovalStatus to, String detail) {
        Map<String, String> data = baseData(va, TYPE_VENDOR_STATUS_CHANGED);
        data.put("fromStatus", from != null ? from.name() : "NEW");
        data.put("toStatus", to.name());

        String title = "Agrément #" + va.getId() + " — " + statusLabel(to);
        String body = "L'agrément fournisseur #" + va.getId() + " est passé de "
                + (from != null ? from.name() : "nouveau") + " à " + to.name() + ".";
        if (detail != null && !detail.isBlank()) {
            body += " Motif : " + detail;
        }

        notify(String.valueOf(va.getOrganizationId()), title, body, data, TYPE_VENDOR_STATUS_CHANGED);
        notify(String.valueOf(va.getFreelancerId()), title, body, data, TYPE_VENDOR_STATUS_CHANGED);
    }

    // ── Emergency revocation notification ─────────────────────

    /**
     * Urgent notification when an agreement is suspended with immediate blocking of active applications.
     */
    public void notifyEmergencyRevoke(VendorApproval va, String reason, int blockedApplicationCount) {
        Map<String, String> data = baseData(va, TYPE_VENDOR_EMERGENCY_REVOKE);
        data.put("blockedApplications", String.valueOf(blockedApplicationCount));

        String title = "URGENT — Agrément #" + va.getId() + " suspendu (révocation d'urgence)";
        String body = "L'agrément fournisseur #" + va.getId() + " a été suspendu avec effet immédiat."
                + " " + blockedApplicationCount + " candidature(s) active(s) ont été bloquées.";
        if (reason != null && !reason.isBlank()) {
            body += " Motif : " + reason;
        }

        notify(String.valueOf(va.getOrganizationId()), title, body, data, TYPE_VENDOR_EMERGENCY_REVOKE);
        notify(String.valueOf(va.getFreelancerId()), title, body, data, TYPE_VENDOR_EMERGENCY_REVOKE);
    }

    // ── Expiry reminder ──────────────────────────────────────

    public void notifyExpiryApproaching(VendorApproval va, int daysLeft) {
        Map<String, String> data = baseData(va, TYPE_VENDOR_EXPIRY_APPROACHING);
        data.put("daysLeft", String.valueOf(daysLeft));
        data.put("validUntil", va.getValidUntil() != null ? va.getValidUntil().toString() : "");
        String body = "L'agrément #" + va.getId() + " expire dans " + daysLeft
                + " jour(s) (fin de validité : " + va.getValidUntil() + ")."
                + " Prévoyez un renouvellement ou une mise à jour avec l'administration.";
        notify(String.valueOf(va.getOrganizationId()),
                "Agrément #" + va.getId() + " — fin de validité proche",
                body, data, TYPE_VENDOR_EXPIRY_APPROACHING);
        notify(String.valueOf(va.getFreelancerId()),
                "Agrément #" + va.getId() + " — fin de validité proche",
                body, data, TYPE_VENDOR_EXPIRY_APPROACHING);
    }

    // ── Helpers ───────────────────────────────────────────────

    private String statusLabel(VendorApprovalStatus s) {
        return switch (s) {
            case PENDING -> "en attente";
            case APPROVED -> "approuvé";
            case REJECTED -> "rejeté";
            case SUSPENDED -> "suspendu";
            case EXPIRED -> "expiré";
        };
    }

    private Map<String, String> baseData(VendorApproval va, String notifType) {
        Map<String, String> data = new HashMap<>();
        data.put("vendorApprovalId", String.valueOf(va.getId()));
        data.put("organizationId", String.valueOf(va.getOrganizationId()));
        data.put("freelancerId", String.valueOf(va.getFreelancerId()));
        data.put("type", notifType);
        return data;
    }

    private void notify(String userId, String title, String body, Map<String, String> data, String type) {
        try {
            notificationClient.create(NotificationRequestDto.builder()
                    .userId(userId)
                    .title(title)
                    .body(body)
                    .type(type)
                    .data(data)
                    .build());
            log.debug("[VENDOR-NOTIF] Sent to userId={} title={}", userId, title);
        } catch (Exception e) {
            log.warn("[VENDOR-NOTIF] Failed for userId={}: {}", userId, e.getMessage());
        }
    }
}
