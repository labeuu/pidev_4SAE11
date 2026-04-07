package org.example.vendor.dto.response;

import lombok.Data;
import org.example.vendor.entity.VendorApprovalStatus;

import java.time.LocalDateTime;

@Data
public class VendorAuditEntryResponse {
    private Long id;
    private Long vendorApprovalId;
    private VendorApprovalStatus fromStatus;
    private VendorApprovalStatus toStatus;
    private String action;
    private Long actorUserId;
    private String detail;
    private LocalDateTime createdAt;
}
