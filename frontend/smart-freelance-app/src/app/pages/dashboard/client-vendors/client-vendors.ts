import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { VendorService, VendorApproval, VendorApprovalStatus } from '../../../core/services/vendor.service';
import { UserService } from '../../../core/services/user.service';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-client-vendors',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './client-vendors.html',
  styleUrl: '../my-vendors/my-vendors.scss',
})
export class ClientVendors implements OnInit {

  approvals: VendorApproval[] = [];
  loading = true;
  organizationId = 0;
  userMap = new Map<number, string>();

  /** Brouillon nom complet pour signature électronique par id d'agrément */
  signatureDraft: Record<number, string> = {};

  constructor(
    private vendorService: VendorService,
    private userService: UserService,
    private auth: AuthService,
    private router: Router,
    private toast: ToastService,
  ) {}

  ngOnInit() {
    if (!this.auth.isClient()) {
      this.router.navigate(['/dashboard']);
      return;
    }
    this.loadUsers();
    this.resolveOrgIdAndLoad();
  }

  /** L’ID organisation = ID utilisateur CLIENT en base (même clé que `user_id` / AuthService). */
  private resolveOrgIdAndLoad() {
    const fromAuth = this.auth.getUserId();
    if (fromAuth != null && fromAuth > 0) {
      this.organizationId = fromAuth;
      this.loadApprovals();
      return;
    }
    this.auth.fetchUserProfile().subscribe(u => {
      this.organizationId = u?.id ?? 0;
      this.loadApprovals();
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

  loadApprovals() {
    if (!this.organizationId) {
      this.loading = false;
      return;
    }
    this.loading = true;
    this.vendorService.getByOrganization(this.organizationId).subscribe(data => {
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

  needsClientSignature(a: VendorApproval): boolean {
    return a.status === 'PENDING' && !a.clientSignedAt;
  }

  submitClientSignature(a: VendorApproval) {
    const name = (this.signatureDraft[a.id] ?? '').trim();
    if (!name) {
      this.toast.error('Indiquez votre nom complet pour signer.');
      return;
    }
    if (!this.organizationId) return;
    this.vendorService
      .signAsClient(a.id, { signerUserId: this.organizationId, fullName: name })
      .subscribe({
        next: () => {
          this.toast.success('Signature enregistrée. Le freelancer doit aussi signer avant validation admin.');
          this.signatureDraft[a.id] = '';
          this.loadApprovals();
        },
        error: (err: { error?: { message?: string } }) =>
          this.toast.error(err?.error?.message ?? 'Signature impossible.'),
      });
  }
}
