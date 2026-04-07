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
  selector: 'app-client-vendors',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './client-vendors.html',
  styleUrl: '../my-vendors/my-vendors.scss',
})
export class ClientVendors implements OnInit {

  approvals: VendorApproval[] = [];
  loading = true;
  loadError: string | null = null;
  organizationId = 0;
  userMap = new Map<number, string>();

  /** Brouillon nom complet pour signature électronique par id d'agrément */
  signatureDraft: Record<number, string> = {};

  constructor(
    private readonly vendorService: VendorService,
    private readonly userService: UserService,
    private readonly auth: AuthService,
    private readonly router: Router,
    private readonly toast: ToastService,
  ) {}

  ngOnInit() {
    if (!this.auth.isClient()) {
      this.router.navigate(['/dashboard'], { replaceUrl: true });
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
    this.auth.fetchUserProfile().subscribe({
      next: u => {
        const resolved = u?.id ?? 0;
        this.organizationId = resolved;
        if (!resolved) {
          this.loading = false;
          const msg = 'Profil client introuvable. Reconnectez-vous.';
          this.loadError = msg;
          this.toast.error(msg);
          return;
        }
        this.loadApprovals();
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

  loadApprovals() {
    if (!this.organizationId || this.organizationId <= 0) {
      this.loading = false;
      return;
    }
    this.loading = true;
    this.loadError = null;
    this.vendorService
      .getByOrganization(this.organizationId)
      .pipe(
        finalize(() => {
          this.loading = false;
        }),
        catchError(err => {
          const msg = parseVendorApiMessage(err, 'Impossible de charger les agréments.');
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
        error: err =>
          this.toast.error(parseVendorApiMessage(err, 'Signature impossible.')),
      });
  }
}
