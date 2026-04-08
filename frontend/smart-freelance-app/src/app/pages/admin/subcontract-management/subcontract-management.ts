import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SubcontractService, Subcontract, SubcontractDashboard } from '../../../core/services/subcontract.service';

@Component({
  selector: 'app-subcontract-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './subcontract-management.html',
  styleUrls: ['./subcontract-management.scss']
})
export class SubcontractManagement implements OnInit {
  subcontracts: Subcontract[] = [];
  filtered: Subcontract[] = [];
  dashboard: SubcontractDashboard | null = null;
  filterStatus = '';
  filterCategory = '';
  loading = true;
  loadingDashboard = true;
  errorMessage = '';
  dashboardError = '';
  selectedSubcontract: Subcontract | null = null;

  statuses = ['', 'DRAFT', 'PROPOSED', 'ACCEPTED', 'REJECTED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'CLOSED'];
  categories = ['', 'DEVELOPMENT', 'DESIGN', 'TESTING', 'CONTENT', 'CONSULTING'];

  constructor(private svc: SubcontractService) {}

  ngOnInit() {
    this.load();
    this.loadDashboard();
  }

  load() {
    this.loading = true;
    this.errorMessage = '';
    this.svc.getAll().subscribe({
      next: data => { this.subcontracts = data; this.applyFilter(); this.loading = false; },
      error: () => {
        this.errorMessage = "Impossible de charger la liste des sous-traitances.";
        this.loading = false;
      }
    });
  }

  loadDashboard() {
    this.loadingDashboard = true;
    this.dashboardError = '';
    this.svc.getDashboard().subscribe({
      next: d => this.dashboard = d,
      error: () => this.dashboardError = "Impossible de charger les indicateurs.",
      complete: () => this.loadingDashboard = false
    });
  }

  applyFilter() {
    this.filtered = this.subcontracts.filter(s =>
      (!this.filterStatus || s.status === this.filterStatus) &&
      (!this.filterCategory || s.category === this.filterCategory)
    );
  }

  select(s: Subcontract) { this.selectedSubcontract = s; }
  closeDetail() { this.selectedSubcontract = null; }

  statusClass(status: string): string {
    const map: Record<string, string> = {
      DRAFT: 'badge-secondary', PROPOSED: 'badge-info', ACCEPTED: 'badge-primary',
      REJECTED: 'badge-danger', IN_PROGRESS: 'badge-warning', COMPLETED: 'badge-success',
      CANCELLED: 'badge-dark', CLOSED: 'badge-light'
    };
    return map[status] || 'badge-secondary';
  }

  progressPercent(s: Subcontract): number {
    return s.totalDeliverables > 0 ? Math.round(s.approvedDeliverables / s.totalDeliverables * 100) : 0;
  }
}
