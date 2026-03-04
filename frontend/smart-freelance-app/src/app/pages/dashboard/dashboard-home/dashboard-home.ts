import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { AuthService } from '../../../core/services/auth.service';
import { Card } from '../../../shared/components/card/card';
import { Project, ProjectService } from '../../../core/services/project.service';
import { UserService } from '../../../core/services/user.service';
import { RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { PortfolioService } from '../../../core/services/portfolio.service';

@Component({
  selector: 'app-dashboard-home',
  imports: [Card, RouterModule, CommonModule],
  templateUrl: './dashboard-home.html',
  styleUrl: './dashboard-home.scss',
  standalone: true,
})
export class DashboardHome implements OnInit{
  constructor(
    public auth: AuthService,
    private ps: ProjectService,
    private us: UserService,
    private portfolioService: PortfolioService,
    private cdr: ChangeDetectorRef
  ) {}

  userRole: string | null = null;
  recommendedProjects: Project[] = [];
  isLoadingRecommendations = false;

  ngOnInit(): void {

    this.userRole = this.auth.getUserRole();
    if (this.userRole === 'FREELANCER') {

      const email = this.auth.getPreferredUsername();

      if (!email) return; // safety check

      this.us.getByEmail(email).subscribe(user => {
        if (user?.id) {
          this.loadRecommendations(user.id);
        }
      });

    }
  }


  loadRecommendations(userId: number): void {

    this.isLoadingRecommendations = true;

    this.ps.getRecommendedProjects(userId)
      .subscribe({
        next: (projects: any[]) => {

          if (!projects || projects.length === 0) {
            this.recommendedProjects = [];
            this.isLoadingRecommendations = false;
            return;
          }

          // 🔥 Enrich each project with real skills
          const enrichedProjects = projects.map(project => {

            if (project.skillIds && project.skillIds.length > 0) {

              // Fetch full skill objects using PortfolioService
              this.portfolioService.getUserSkills(userId).subscribe(skills => {

                // Match only skills used in this project
                project.skills = skills.filter(skill =>
                  project.skillIds.includes(skill.id)
                );

                this.cdr.detectChanges();
              });

            }

            return project;
          });

          this.recommendedProjects = enrichedProjects;
          this.isLoadingRecommendations = false;
        },

        error: () => {
          this.isLoadingRecommendations = false;
        }
      });
  }

}
