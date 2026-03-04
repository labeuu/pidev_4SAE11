import { Component, OnInit, signal, computed, inject, ChangeDetectorRef } from '@angular/core';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { ProjectService, Project } from '../../../core/services/project.service';
import { UserService } from '../../../core/services/user.service';
import { ContractService } from '../../../core/services/contract.service';
import { OfferService } from '../../../core/services/offer.service';
import { ProjectApplicationService } from '../../../core/services/project-application.service';
import { ProjectsFeed } from '../../../shared/components/projects-feed/projects-feed.component';
import { ProjectFeed } from '../../../shared/models/project-feed';

export type SortOption = 'newest' | 'oldest' | 'budget-high' | 'budget-low';

@Component({
  selector: 'app-freelancer-home',
  standalone: true,
  imports: [ProjectsFeed, RouterLink, CommonModule, FormsModule],
  templateUrl: './freelancer-home.html',
  styleUrl: './freelancer-home.scss',
})
export class FreelancerHome implements OnInit {
  private projectService = inject(ProjectService);
  private auth = inject(AuthService);
  private us = inject(UserService);
  private contractService = inject(ContractService);
  private offerService = inject(OfferService);
  private applicationService = inject(ProjectApplicationService);
  private cdr = inject(ChangeDetectorRef);

  displayName = '';
  myApplicationsCount = 0;
  activeContractsCount = 0;
  myOffersCount = 0;
  isLoadingStats = true;
  recommendedProjects: Project[] = [];
  isLoadingRecommendations = false;

  // Feed state
  private allProjects = signal<ProjectFeed[]>([]);
  searchTerm = '';
  sortOption: SortOption = 'newest';
  currentPage = signal(1);
  readonly pageSize = 6;
  isFetching = signal(true);

  filteredProjects = computed(() => {
    const list = this.allProjects();
    const q = (this.searchTerm || '').trim().toLowerCase();
    if (!q) return list;
    return list.filter(
      (p) =>
        p.title.toLowerCase().includes(q) ||
        (p.description || '').toLowerCase().includes(q) ||
        (p.category || '').toLowerCase().includes(q) ||
        p.skills.some((s) => s.toLowerCase().includes(q))
    );
  });

  sortedProjects = computed(() => {
    const list = [...this.filteredProjects()];
    const toMs = (p: ProjectFeed) => new Date(p.createdAt || 0).getTime();
    switch (this.sortOption) {
      case 'newest': return list.sort((a, b) => toMs(b) - toMs(a));
      case 'oldest': return list.sort((a, b) => toMs(a) - toMs(b));
      case 'budget-high': return list.sort((a, b) => b.budget - a.budget);
      case 'budget-low': return list.sort((a, b) => a.budget - b.budget);
      default: return list;
    }
  });

  totalCount = computed(() => this.sortedProjects().length);
  totalPages = computed(() => Math.max(1, Math.ceil(this.totalCount() / this.pageSize)));
  startIndex = computed(() => (this.currentPage() - 1) * this.pageSize);
  endIndex = computed(() => Math.min(this.startIndex() + this.pageSize, this.totalCount()));
  displayedProjects = computed(() => this.sortedProjects().slice(this.startIndex(), this.endIndex()));

  ngOnInit(): void {
    this.displayName = this.auth.getDisplayName() || 'Freelancer';
    const email = this.auth.getPreferredUsername();

    if (email) {
      this.us.getByEmail(email).subscribe((user) => {
        if (user?.id) {
          this.loadFreelancerStats(user.id);
          this.loadRecommendations(user.id);
        }
      });
    }

    this.loadAllProjects();
  }

  private loadFreelancerStats(freelancerId: number): void {
    this.isLoadingStats = true;
    forkJoin({
      applications: this.applicationService.getApplicationsByFreelance(freelancerId).pipe(catchError(() => of([]))),
      contracts: this.contractService.getByFreelancer(freelancerId).pipe(catchError(() => of([]))),
      offers: this.offerService.getOffersByFreelancer(freelancerId).pipe(catchError(() => of([]))),
    }).subscribe({
      next: ({ applications, contracts, offers }) => {
        this.myApplicationsCount = applications?.length ?? 0;
        const active = contracts?.filter(c => c.status === 'ACTIVE' || c.status === 'PENDING_SIGNATURE') ?? [];
        this.activeContractsCount = active.length;
        this.myOffersCount = offers?.length ?? 0;
        this.isLoadingStats = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.isLoadingStats = false;
        this.cdr.detectChanges();
      },
    });
  }

  private loadRecommendations(userId: number): void {
    this.isLoadingRecommendations = true;
    this.projectService.getRecommendedProjects(userId).pipe(
      catchError(() => of([]))
    ).subscribe((projects) => {
      this.recommendedProjects = projects ?? [];
      this.isLoadingRecommendations = false;
      this.cdr.detectChanges();
    });
  }

  private loadAllProjects(): void {
    forkJoin({
      projects: this.projectService.getAllProjects(),
      users: this.us.getAll(),
    }).subscribe({
      next: ({ projects, users }) => {
        const userMap = new Map<number, { firstName: string; lastName: string; avatarUrl?: string }>();
        (users ?? []).forEach((u) => userMap.set(u.id, { firstName: u.firstName, lastName: u.lastName, avatarUrl: u.avatarUrl }));
        const open = (projects ?? []).filter(p => !p.status || p.status === 'OPEN');
        this.allProjects.set(open.map((p, i) => this.toFeed(p, i, userMap)));
        this.isFetching.set(false);
      },
      error: () => this.isFetching.set(false),
    });
  }

  private toFeed(p: Project, index: number, userMap: Map<number, { firstName: string; lastName: string; avatarUrl?: string }>): ProjectFeed {
    const owner = p.clientId != null ? userMap.get(p.clientId) : undefined;
    const fullName = owner ? `${owner.firstName} ${owner.lastName}`.trim() || 'Project Owner' : 'Project Owner';
    const avatarUrl = owner?.avatarUrl;
    return {
      id: String(p.id ?? index),
      title: p.title,
      budget: p.budget ?? 0,
      client: {
        name: fullName,
        avatar: avatarUrl || `https://api.dicebear.com/7.x/avataaars/svg?seed=${p.clientId ?? index}`,
        rating: Number.parseFloat((4.2 + Math.random() * 0.8).toFixed(1)),
      },
      description: p.description,
      skills: this.parseSkills(p.skills),
      postedAgo: this.timeAgo(p.createdAt),
      createdAt: p.createdAt,
      category: p.category,
    };
  }

  onSearchChange(): void { this.currentPage.set(1); }
  onSortChange(): void { this.currentPage.set(1); }
  goToPage(page: number): void {
    const total = this.totalPages();
    if (page >= 1 && page <= total) this.currentPage.set(page);
  }

  private parseSkills(s: unknown): string[] {
    if (!s) return [];
    if (Array.isArray(s)) {
      return s.map((x) => (typeof x === 'object' && x && 'name' in x ? (x as { name: string }).name : String(x))).filter(Boolean);
    }
    return String(s).split(',').map(x => x.trim()).filter(Boolean);
  }

  formatSkills(s: unknown): string {
    if (!s) return '';
    const arr = this.parseSkills(s);
    return arr.slice(0, 3).join(', ');
  }

  private timeAgo(dateStr?: string): string {
    if (!dateStr) return 'recently';
    const diff = Date.now() - new Date(dateStr).getTime();
    const h = Math.floor(diff / 3_600_000);
    const d = Math.floor(h / 24);
    if (d > 0) return `${d}d ago`;
    if (h > 0) return `${h}h ago`;
    return 'just now';
  }
}
