import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { catchError, finalize } from 'rxjs/operators';
import { of } from 'rxjs';
import { VendorService, VendorApproval, VendorApprovalStatus } from '../../../core/services/vendor.service';
import { UserService } from '../../../core/services/user.service';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { parseVendorApiMessage } from '../../../core/utils/vendor-api-message';
import {
  formatVendorShortDate,
  vendorApprovalStatusClass,
  vendorApprovalStatusLabel,
} from '../../../core/utils/vendor-ui.helpers';

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
  loadError: string | null = null;
  userId = 0;
  userMap = new Map<number, string>();

  signatureDraft: Record<number, string> = {};

  constructor(
    private readonly vendorService: VendorService,
    private readonly userService: UserService,
    private readonly auth: AuthService,
    private readonly router: Router,
    private readonly toast: ToastService,
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
    this.auth.fetchUserProfile().subscribe({
      next: u => {
        const resolved = u?.id ?? 0;
        this.userId = resolved;
        if (!resolved) {
          this.loading = false;
          const msg = 'Profil utilisateur introuvable. Reconnectez-vous.';
          this.loadError = msg;
          this.toast.error(msg);
          return;
        }
        this.loadMyApprovals();
      },
      error: () => {
        this.loading = false;
        const msg = 'Impossible de charger votre profil.';
        this.loadError = msg;
        this.toast.error(msg);
      },
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
    if (!this.userId || this.userId <= 0) {
      this.loading = false;
      return;
    }
    this.loading = true;
    this.loadError = null;
    this.vendorService
      .getByFreelancer(this.userId)
      .pipe(
        finalize(() => {
          this.loading = false;
        }),
        catchError(err => {
          const msg = parseVendorApiMessage(err, 'Impossible de charger vos agréments.');
          this.loadError = msg;
          this.toast.error(msg);
          return of([] as VendorApproval[]);
        }),
      )
      .subscribe(data => {
        this.approvals = data;
      });
  }

  formatShortDate = formatVendorShortDate;

  get activeCount(): number {
    return this.approvals.filter(a => a.status === 'APPROVED' && a.isActive).length;
  }

  get pendingCount(): number {
    return this.approvals.filter(a => a.status === 'PENDING').length;
  }

  statusClass(status: VendorApprovalStatus): string {
    return vendorApprovalStatusClass(status);
  }

  statusLabel(status: VendorApprovalStatus): string {
    return vendorApprovalStatusLabel(status);
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
        error: err =>
          this.toast.error(parseVendorApiMessage(err, 'Signature impossible.')),
      });
  }
}
