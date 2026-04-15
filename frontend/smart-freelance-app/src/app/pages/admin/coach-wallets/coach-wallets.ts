import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  CoachWalletDto,
  CoachWalletService,
  WalletTransactionDto,
} from '../../../core/services/coach-wallet.service';
import { User, UserService } from '../../../core/services/user.service';
import { AuthService } from '../../../core/services/auth.service';

type AdminTab = 'overview' | 'credit' | 'audit';
type WalletStatusFilter = 'ALL' | 'ACTIVE' | 'LOW' | 'BLOCKED';

@Component({
  selector: 'app-coach-wallets',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './coach-wallets.html',
  styleUrl: './coach-wallets.scss',
})
export class CoachWallets implements OnInit {
  tab: AdminTab = 'overview';
  adminId = 0;

  wallets: CoachWalletDto[] = [];
  walletsTotal = 0;
  walletsPage = 0;
  walletsPageSize = 15;
  walletsLoading = false;

  statusFilter: WalletStatusFilter = 'ALL';
  balanceMin: number | null = null;
  balanceMax: number | null = null;
  searchUserText: string = '';

  users: User[] = [];
  creditUserSearch = '';
  creditUserId: number | null = null;
  creditAmount = 500;
  creditReason = 'BONUS_BIENVENUE';
  creditBusy = false;
  showCreditConfirm = false;
  creditPreviewFrom = 0;
  creditPreviewLoading = false;

  auditRows: WalletTransactionDto[] = [];
  auditTotal = 0;
  auditPage = 0;
  auditPageSize = 25;
  auditLoading = false;
  auditTypeFilter = '';

  blockBusyId: number | null = null;

  readonly creditReasons = ['BONUS_BIENVENUE', 'COMPENSATION', 'PROMOTION', 'AUTRE'] as const;

  constructor(
    private coachApi: CoachWalletService,
    private usersApi: UserService,
    private auth: AuthService,
  ) {}

  ngOnInit(): void {
    this.adminId = this.auth.getUserId() ?? 0;
    this.loadWallets();
    this.usersApi.getAll().subscribe((u) => (this.users = u || []));
  }

  get filteredUsers(): User[] {
    const q = this.creditUserSearch.trim().toLowerCase();
    const freelancers = this.users.filter((u) => u.role === 'FREELANCER');
    if (!q) return freelancers.slice(0, 40);
    return freelancers
      .filter(
        (u) =>
          String(u.id).includes(q) ||
          (u.email && u.email.toLowerCase().includes(q)) ||
          `${u.firstName} ${u.lastName}`.toLowerCase().includes(q),
      )
      .slice(0, 50);
  }

  get selectedCreditUser(): User | null {
    if (!this.creditUserId) return null;
    return this.users.find((u) => u.id === this.creditUserId) ?? null;
  }

  switchTab(t: AdminTab): void {
    this.tab = t;
    if (t === 'audit' && this.auditRows.length === 0) {
      this.loadAudit();
    }
  }

  loadWallets(): void {
    if (!this.adminId) return;
    this.walletsLoading = true;
    this.coachApi.adminListWallets(this.adminId, this.walletsPage, this.walletsPageSize).subscribe({
      next: (p) => {
        this.wallets = p.content || [];
        this.walletsTotal = p.totalElements ?? 0;
        this.walletsLoading = false;
      },
      error: () => {
        this.wallets = [];
        this.walletsLoading = false;
      },
    });
  }

  loadAudit(): void {
    if (!this.adminId) return;
    this.auditLoading = true;
    this.coachApi.adminAuditLog(this.adminId, this.auditPage, this.auditPageSize).subscribe({
      next: (p) => {
        this.auditRows = p.content || [];
        this.auditTotal = p.totalElements ?? 0;
        this.auditLoading = false;
      },
      error: () => {
        this.auditRows = [];
        this.auditLoading = false;
      },
    });
  }

  get filteredWallets(): CoachWalletDto[] {
    let list = [...this.wallets];
    if (this.statusFilter === 'LOW') {
      list = list.filter((w) => w.balance <= 300 && !w.blocked);
    } else if (this.statusFilter === 'BLOCKED') {
      list = list.filter((w) => w.blocked);
    } else if (this.statusFilter === 'ACTIVE') {
      list = list.filter((w) => !w.blocked && w.balance > 300);
    }
    if (this.balanceMin != null) list = list.filter((w) => w.balance >= this.balanceMin!);
    if (this.balanceMax != null) list = list.filter((w) => w.balance <= this.balanceMax!);
    const q = this.searchUserText.trim().toLowerCase();
    if (q) {
      list = list.filter((w) => {
        const name = this.userDisplayName(w.userId).toLowerCase();
        const email = this.userDisplayEmail(w.userId).toLowerCase();
        return name.includes(q) || email.includes(q);
      });
    }
    return list;
  }

  /** KPIs approximatifs sur la page courante + total global */
  get kpiTotalWallets(): number {
    return this.walletsTotal;
  }

  get kpiLowOnPage(): number {
    return this.wallets.filter((w) => w.balance <= 300 && !w.blocked).length;
  }

  get kpiBlockedOnPage(): number {
    return this.wallets.filter((w) => w.blocked).length;
  }

  walletStatusLabel(w: CoachWalletDto): string {
    if (w.blocked) return 'BLOQUÉ';
    if (w.balance <= 300) return 'FAIBLE';
    return 'ACTIF';
  }

  statusClass(w: CoachWalletDto): string {
    if (w.blocked) return 'st-blocked';
    if (w.balance <= 300) return 'st-low';
    return 'st-ok';
  }

  changeWalletsPage(delta: number): void {
    const next = this.walletsPage + delta;
    if (next < 0) return;
    const maxPage = Math.max(0, Math.ceil(this.walletsTotal / this.walletsPageSize) - 1);
    if (next > maxPage) return;
    this.walletsPage = next;
    this.loadWallets();
  }

  changeAuditPage(delta: number): void {
    const next = this.auditPage + delta;
    if (next < 0) return;
    const maxPage = Math.max(0, Math.ceil(this.auditTotal / this.auditPageSize) - 1);
    if (next > maxPage) return;
    this.auditPage = next;
    this.loadAudit();
  }

  get filteredAudit(): WalletTransactionDto[] {
    if (!this.auditTypeFilter) return this.auditRows;
    return this.auditRows.filter((r) => r.type === this.auditTypeFilter);
  }

  openCreditConfirm(): void {
    if (!this.adminId || !this.creditUserId || this.creditAmount < 1 || !this.creditReason.trim()) return;
    this.creditPreviewLoading = true;
    this.coachApi.adminGetWallet(this.adminId, this.creditUserId).subscribe({
      next: (w) => {
        this.creditPreviewFrom = w.balance;
        this.creditPreviewLoading = false;
        this.showCreditConfirm = true;
      },
      error: () => {
        this.creditPreviewFrom = 0;
        this.creditPreviewLoading = false;
        this.showCreditConfirm = true;
      },
    });
  }

  confirmCredit(): void {
    if (!this.adminId || !this.creditUserId) return;
    this.creditBusy = true;
    this.coachApi
      .adminCredit(this.adminId, this.creditUserId, {
        amount: this.creditAmount,
        reason: this.creditReason,
      })
      .subscribe({
        next: () => {
          this.creditBusy = false;
          this.showCreditConfirm = false;
          this.loadWallets();
          this.auditRows = [];
          this.loadAudit();
        },
        error: () => {
          this.creditBusy = false;
        },
      });
  }

  quickCredit(w: CoachWalletDto): void {
    this.tab = 'credit';
    this.creditUserId = w.userId;
    this.creditUserSearch = String(w.userId);
  }

  toggleBlock(w: CoachWalletDto): void {
    if (!this.adminId) return;
    this.blockBusyId = w.userId;
    this.coachApi.adminSetBlocked(this.adminId, w.userId, !w.blocked).subscribe({
      next: () => {
        this.blockBusyId = null;
        this.loadWallets();
      },
      error: () => (this.blockBusyId = null),
    });
  }

  exportAuditCsv(): void {
    const rows = this.filteredAudit;
    const header = ['createdAt', 'type', 'amount', 'balanceBefore', 'balanceAfter', 'reason', 'role'];
    const lines = [header.join(',')];
    for (const r of rows) {
      lines.push(
        [
          r.createdAt,
          r.type,
          r.amount,
          r.balanceBefore,
          r.balanceAfter,
          (r.reason || '').replace(/,/g, ';'),
          r.performedByRole || '',
        ].join(','),
      );
    }
    const blob = new Blob([lines.join('\n')], { type: 'text/csv;charset=utf-8' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = `coach-wallet-audit-${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    URL.revokeObjectURL(a.href);
  }

  userLabel(id: number): string {
    const u = this.users.find((x) => x.id === id);
    if (!u) return '#' + id;
    return `${u.firstName} ${u.lastName} (${u.email})`;
  }

  userDisplayName(id: number): string {
    const u = this.users.find((x) => x.id === id);
    if (!u) return 'Utilisateur inconnu';
    return `${u.firstName} ${u.lastName}`;
  }

  userDisplayEmail(id: number): string {
    const u = this.users.find((x) => x.id === id);
    return u?.email ?? '';
  }

  get walletPageCount(): number {
    return Math.max(1, Math.ceil(this.walletsTotal / this.walletsPageSize) || 1);
  }

  get auditPageCount(): number {
    return Math.max(1, Math.ceil(this.auditTotal / this.auditPageSize) || 1);
  }
}
