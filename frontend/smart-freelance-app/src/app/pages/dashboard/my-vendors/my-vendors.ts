import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { VendorService, VendorApproval, VendorApprovalStatus } from '../../../core/services/vendor.service';
import { UserService } from '../../../core/services/user.service';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-my-vendors',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './my-vendors.html',
  styleUrl: './my-vendors.scss',
})
export class MyVendors implements OnInit {

  approvals: VendorApproval[] = [];
  loading = true;
  userId = 0;
  userMap = new Map<number, string>();

  signatureDraft: Record<number, string> = {};

  constructor(
    private vendorService: VendorService,
    private userService: UserService,
    private auth: AuthService,
    private router: Router,
    private toast: ToastService,
  ) {}

  ngOnInit() {
    if (this.auth.isClient()) {
      this.router.navigate(['/dashboard/client-vendors'], { replaceUrl: true });
      return;
    }
    this.loadUsers();
    this.resolveFreelancerIdAndLoad();
  }

  private resolveFreelancerIdAndLoad() {
    const id = this.auth.getUserId();
    if (id != null && id > 0) {
      this.userId = id;
      this.loadMyApprovals();
      return;
    }
    this.auth.fetchUserProfile().subscribe(u => {
      this.userId = u?.id ?? 0;
      this.loadMyApprovals();
    });
  }

  loadUsers() {
    this.userService.getAll().subscribe(users => {
      if (!users) return;
      users.forEach(u => this.userMap.set(u.id, `${u.firstName} ${u.lastName}`));
    });
  }

  userName(id: number): string {
    return this.userMap.get(id) || `#${id}`;
  }

  loadMyApprovals() {
    this.loading = true;
    this.vendorService.getByFreelancer(this.userId).subscribe(data => {
      this.approvals = data;
      this.loading = false;
    });
  }

  get activeCount(): number {
    return this.approvals.filter(a => a.status === 'APPROVED' && a.isActive).length;
  }

  get pendingCount(): number {
    return this.approvals.filter(a => a.status === 'PENDING').length;
  }

  statusClass(status: VendorApprovalStatus): string {
    const map: Record<VendorApprovalStatus, string> = {
      PENDING: 'badge--warning',
      APPROVED: 'badge--success',
      REJECTED: 'badge--error',
      SUSPENDED: 'badge--error',
      EXPIRED: 'badge--neutral',
    };
    return map[status] || '';
  }

  statusLabel(status: VendorApprovalStatus): string {
    const map: Record<VendorApprovalStatus, string> = {
      PENDING: 'En attente',
      APPROVED: 'Approuvé',
      REJECTED: 'Rejeté',
      SUSPENDED: 'Suspendu',
      EXPIRED: 'Expiré',
    };
    return map[status] || status;
  }

  resubmit(a: VendorApproval) {
    this.vendorService.resubmit(a.id).subscribe(() => this.loadMyApprovals());
  }

  needsFreelancerSignature(a: VendorApproval): boolean {
    return a.status === 'PENDING' && !a.freelancerSignedAt;
  }

  submitFreelancerSignature(a: VendorApproval) {
    const name = (this.signatureDraft[a.id] ?? '').trim();
    if (!name) {
      this.toast.error('Indiquez votre nom complet pour signer.');
      return;
    }
    if (!this.userId) return;
    this.vendorService
      .signAsFreelancer(a.id, { signerUserId: this.userId, fullName: name })
      .subscribe({
        next: () => {
          this.toast.success('Signature enregistrée. Le client doit aussi signer avant validation admin.');
          this.signatureDraft[a.id] = '';
          this.loadMyApprovals();
        },
        error: (err: { error?: { message?: string } }) =>
          this.toast.error(err?.error?.message ?? 'Signature impossible.'),
      });
  }
}
