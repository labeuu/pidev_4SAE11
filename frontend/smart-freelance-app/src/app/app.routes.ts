import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role-guard';

export const routes: Routes = [
  // Public routes with layout
  {
    path: '',
    loadComponent: () => import('./layouts/public-layout/public-layout').then(m => m.PublicLayout),
    children: [
      { path: '', loadComponent: () => import('./pages/public/home/home').then(m => m.Home) },
      { path: 'how-it-works', loadComponent: () => import('./pages/public/how-it-works/how-it-works').then(m => m.HowItWorks) },
      { path: 'about', loadComponent: () => import('./pages/public/about/about').then(m => m.About) },
    ]
  },

  // Auth pages (no layout)
  { path: 'login', loadComponent: () => import('./pages/login/login.component').then(m => m.LoginComponent) },
  { path: 'signup', loadComponent: () => import('./pages/signup/signup.component').then(m => m.SignupComponent) },
  { path: 'forgot-password', loadComponent: () => import('./pages/forgot-password/forgot-password.component').then(m => m.ForgotPasswordComponent) },

  // Dashboard routes (protected; CLIENT and FREELANCER only — admin uses /admin/*)
  {
    path: 'dashboard',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['CLIENT', 'FREELANCER'] },
    loadComponent: () => import('./layouts/dashboard-layout/dashboard-layout').then(m => m.DashboardLayout),
    children: [
      { path: '', loadComponent: () => import('./pages/dashboard/dashboard-home/dashboard-home').then(m => m.DashboardHome) },
      // Redirects for clearer UX
      { path: 'post-job', redirectTo: 'my-projects/add', pathMatch: 'full' },
      { path: 'browse-freelancers', redirectTo: 'browse-offers', pathMatch: 'full' },
      {
        path: 'my-projects',
        children: [
          // List / overview of all projects
          {
            path: '',
            pathMatch: 'full',
            loadComponent: () =>
              import('./pages/projects/list-projects/list-projects')
                .then(m => m.ListProjects),
            // title: 'My Projects'   ← optional – can be used by title service
          },

          // Create new project
          {
            path: 'add',
            loadComponent: () =>
              import('./pages/projects/add-project/add-project')
                .then(m => m.AddProject),
            title: 'Add New Project'
          },

          // View single project details
          {
            path: ':id/show',
            loadComponent: () =>
              import('./pages/projects/show-project/show-project')
                .then(m => m.ShowProject),
            title: 'Project Details'
          },

          // Edit existing project
          {
            path: ':id/edit',
            loadComponent: () =>
              import('./pages/projects/update-project/update-project')
                .then(m => m.UpdateProject),
            title: 'Edit Project'
          },
        ]
      },
      { path: 'progress-updates', loadComponent: () => import('./pages/dashboard/progress-updates/progress-updates').then(m => m.ProgressUpdates) },
      { path: 'calendar', loadComponent: () => import('./pages/dashboard/calendar/calendar').then(m => m.Calendar), title: 'Calendar' },
      { path: 'my-tasks', loadComponent: () => import('./pages/dashboard/my-tasks/my-tasks').then(m => m.MyTasks) },
      { path: 'project-tasks', loadComponent: () => import('./pages/dashboard/project-tasks/project-tasks').then(m => m.ProjectTasks) },
      { path: 'github', loadComponent: () => import('./pages/dashboard/github/github').then(m => m.Github), title: 'GitHub' },
      { path: 'track-progress', loadComponent: () => import('./pages/dashboard/track-progress/track-progress').then(m => m.TrackProgress) },
      {
        path: 'my-offers',
        children: [
          { path: '', pathMatch: 'full', loadComponent: () => import('./pages/dashboard/my-offers/list-offers').then(m => m.ListOffers) },
          { path: 'add', loadComponent: () => import('./pages/dashboard/my-offers/add-offer').then(m => m.AddOffer) },
          { path: ':id/edit', loadComponent: () => import('./pages/dashboard/my-offers/edit-offer').then(m => m.EditOffer) },
          { path: ':id/show', loadComponent: () => import('./pages/dashboard/my-offers/show-offer').then(m => m.ShowOffer) },
        ]
      },
      {
        path: 'browse-offers',
        children: [
          { path: '', pathMatch: 'full', loadComponent: () => import('./pages/dashboard/browse-offers/browse-offers').then(m => m.BrowseOffers) },
          { path: ':id', loadComponent: () => import('./pages/dashboard/browse-offers/offer-detail').then(m => m.OfferDetail) },
        ]
      },
      { path: 'my-offer-applications', loadComponent: () => import('./pages/dashboard/my-offer-applications/my-offer-applications').then(m => m.MyOfferApplications) },
      { path: 'design-brief', loadComponent: () => import('./pages/dashboard/design-brief/design-brief').then(m => m.DesignBrief), title: 'Design Brief Builder' },
      {
        path: 'tickets',
        title: 'Support',
        children: [
          { path: '', pathMatch: 'full', loadComponent: () => import('./pages/dashboard/tickets/ticket-user/ticket-user').then(m => m.TicketUser), title: 'My tickets' },
          { path: 'new', loadComponent: () => import('./pages/dashboard/tickets/ticket-form/ticket-form').then(m => m.TicketForm), title: 'New ticket' },
          { path: ':id/edit', loadComponent: () => import('./pages/dashboard/tickets/ticket-form/ticket-form').then(m => m.TicketForm), title: 'Edit ticket' },
          { path: ':id', loadComponent: () => import('./pages/dashboard/tickets/ticket-detail/ticket-detail').then(m => m.TicketDetail), title: 'Ticket' },
        ]
      },
      {
        path: 'reviews',
        children: [
          { path: '', pathMatch: 'full', loadComponent: () => import('./pages/dashboard/reviews/my-reviews/my-reviews').then(m => m.MyReviews) },
          { path: 'about-me', loadComponent: () => import('./pages/dashboard/reviews/reviews-about-me/reviews-about-me').then(m => m.ReviewsAboutMe) },
          { path: 'add', loadComponent: () => import('./pages/dashboard/reviews/add-review/add-review').then(m => m.AddReview) },
          { path: ':id/edit', loadComponent: () => import('./pages/dashboard/reviews/edit-review/edit-review').then(m => m.EditReview) },
        ]
      },
     {
        path: 'browse-jobs',
        children: [
          // List / overview of all projects
          {
            path: '',
            pathMatch: 'full',
            loadComponent: () =>
              import('./pages/projects/list-projects/list-projects')
                .then(m => m.ListProjects),
            // title: 'My Projects'   ← optional – can be used by title service
          },

          // View single project details
          {
            path: ':id/show',
            loadComponent: () =>
              import('./pages/projects/show-project/show-project')
                .then(m => m.ShowProject),
            title: 'Project Details'
          },
        ]
      },

      {
        path: 'my-applications',
        children: [
          // List / overview of all applications
          {
            path: '',
            pathMatch: 'full',
            loadComponent: () =>
              import('./pages/projects-applications/list-application/list-application')
                .then(m => m.ListApplication),
            // title: 'My Applications'   ← optional – can be used by title service
          },

          // Create new application
          {
            path: 'add/:id',
            loadComponent: () =>
              import('./pages/projects-applications/add-application/add-application')
                .then(m => m.AddApplication),
            title: 'Add New Application'
          },

          // View single application details
          {
            path: ':id/show',
            loadComponent: () =>
              import('./pages/projects-applications/show-application/show-application')
                .then(m => m.ShowApplication),
            title: 'Application Details'
          },

          // Edit existing application
          {
            path: ':id/edit',
            loadComponent: () =>
              import('./pages/projects-applications/update-application/update-application')
                .then(m => m.UpdateApplication),
            title: 'Edit Application'
          },
        ]
      },
      { path: 'my-portfolio', loadComponent: () => import('./pages/dashboard/portfolio-overview/portfolio-overview').then(m => m.PortfolioOverview) },
      { path: 'messages', loadComponent: () => import('./pages/chat/chat-page/chat-page.component').then(m => m.ChatPageComponent), title: 'Messages' },
      { path: 'gamification', loadComponent: () => import('./pages/dashboard/gamification/gamification').then(m => m.GamificationPage), title: 'Growth & achievements' },
      { path: 'leaderboard', loadComponent: () => import('./pages/dashboard/leaderboard/leaderboard').then(m => m.LeaderboardPage), title: 'Global Leaderboard' },
      { path: 'notifications', loadComponent: () => import('./pages/dashboard/notifications/notifications').then(m => m.Notifications) },
      { path: 'profile', loadComponent: () => import('./pages/dashboard/profile/profile').then(m => m.Profile) },
      { path: 'settings', redirectTo: 'profile', pathMatch: 'full' },
      { path: 'my-vendors', loadComponent: () => import('./pages/dashboard/my-vendors/my-vendors').then(m => m.MyVendors), title: 'Mes Agréments' },
      { path: 'client-vendors', loadComponent: () => import('./pages/dashboard/client-vendors/client-vendors').then(m => m.ClientVendors), title: 'Mes fournisseurs' },
      { path: 'my-subcontracts', loadComponent: () => import('./pages/dashboard/my-subcontracts/my-subcontracts').then(m => m.MySubcontracts), title: 'Mes Sous-Traitances' },
      { path: 'subcontractor-work', loadComponent: () => import('./pages/dashboard/subcontractor-work/subcontractor-work').then(m => m.SubcontractorWork), title: 'Travaux sous-traités' },
      // Freelancia Jobs (CLIENT: manage own jobs; FREELANCER: browse open jobs)
      {
        path: 'my-jobs',
        children: [
          { path: '', pathMatch: 'full', loadComponent: () => import('./pages/freelancia-jobs/list-jobs/list-jobs').then(m => m.ListJobs), title: 'My Jobs' },
          { path: 'add', loadComponent: () => import('./pages/freelancia-jobs/add-job/add-job').then(m => m.AddJob), title: 'Post a Job' },
          { path: ':id/show', loadComponent: () => import('./pages/freelancia-jobs/show-job/show-job').then(m => m.ShowJob), title: 'Job Details' },
          { path: ':id/edit', loadComponent: () => import('./pages/freelancia-jobs/update-job/update-job').then(m => m.UpdateJob), title: 'Edit Job' },
        ]
      },
      {
        path: 'browse-freelancia-jobs',
        children: [
          { path: '', pathMatch: 'full', loadComponent: () => import('./pages/freelancia-jobs/list-jobs/list-jobs').then(m => m.ListJobs), title: 'Browse Jobs' },
          { path: ':id/show', loadComponent: () => import('./pages/freelancia-jobs/show-job/show-job').then(m => m.ShowJob), title: 'Job Details' },
        ]
      },
      // Freelancia Job Applications
      {
        path: 'my-job-applications',
        children: [
          { path: '', pathMatch: 'full', loadComponent: () => import('./pages/freelancia-job-applications/list-application/list-application').then(m => m.ListApplication), title: 'My Job Applications' },
          { path: 'add/:id', loadComponent: () => import('./pages/freelancia-job-applications/add-application/add-application').then(m => m.AddApplication), title: 'Apply for Job' },
          { path: ':id/show', loadComponent: () => import('./pages/freelancia-job-applications/show-application/show-application').then(m => m.ShowApplication), title: 'Application Details' },
          { path: ':id/edit', loadComponent: () => import('./pages/freelancia-job-applications/update-application/update-application').then(m => m.UpdateApplication), title: 'Edit Application' },
        ]
      },
      { path: 'my-contracts', loadComponent: () => import('./pages/dashboard/my-contracts/my-contracts').then(m => m.MyContracts) },
      { path: 'my-contracts/:id', loadComponent: () => import('./pages/dashboard/my-contracts/contract-detail/contract-detail').then(m => m.ContractDetail) },
      {
        path: 'meetings',
        children: [
          { path: '', pathMatch: 'full', loadComponent: () => import('./pages/meetings/my-meetings/my-meetings').then(m => m.MyMeetings), title: 'My Meetings' },
          { path: 'schedule', loadComponent: () => import('./pages/meetings/schedule-meeting/schedule-meeting').then(m => m.ScheduleMeeting), title: 'Schedule a Meeting' },
          { path: ':meetingId', loadComponent: () => import('./pages/meetings/meeting-detail/meeting-detail').then(m => m.MeetingDetail), title: 'Meeting Details' },
        ]
      },
      { path: 'freelancer-search', loadComponent: () => import('./pages/dashboard/freelancer-search/freelancer-search').then(m => m.FreelancerSearch), title: 'Find Freelancers' },
      { path: 'freelancer-portfolio/:id', loadComponent: () => import('./pages/dashboard/freelancer-portfolio/freelancer-portfolio').then(m => m.DashboardFreelancerPortfolio), title: 'Freelancer Profile' },
    ]
  },

  // Admin routes (protected, ADMIN only)
  {
    path: 'admin',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN'] },
    loadComponent: () => import('./layouts/admin-layout/admin-layout').then(m => m.AdminLayout),
    children: [
      { path: '', loadComponent: () => import('./pages/admin/admin-dashboard/admin-dashboard').then(m => m.AdminDashboard) },
      // Placeholder routes for future implementation
      { path: 'users', loadComponent: () => import('./pages/admin/user-management/user-management').then(m => m.UserManagement) },
      { path: 'contracts', loadComponent: () => import('./pages/admin/admin-contracts/admin-contracts').then(m => m.AdminContracts) },

    
      { path: 'offers', loadComponent: () => import('./pages/admin/offer-management/offer-management').then(m => m.OfferManagement) },
      { path: 'vendors', loadComponent: () => import('./pages/admin/vendor-management/vendor-management').then(m => m.VendorManagement), title: 'Vendor Management' },
      { path: 'subcontracts', loadComponent: () => import('./pages/admin/subcontract-management/subcontract-management').then(m => m.SubcontractManagement), title: 'Sous-Traitance' },
      {
        path: 'projects',
        children: [
          // List / overview of all projects
          {
            path: '',
            pathMatch: 'full',
            loadComponent: () =>
              import('./pages/projects/list-projects/list-projects')
                .then(m => m.ListProjects),
            // title: 'My Projects'   ← optional – can be used by title service
          },

          // View single project details
          {
            path: ':id/show',
            loadComponent: () =>
              import('./pages/projects/show-project/show-project')
                .then(m => m.ShowProject),
            title: 'Project Details'
          },
        ]
      },

      { path: 'planning', loadComponent: () => import('./pages/admin/planning-management/planning-management').then(m => m.PlanningManagement) },
      { path: 'tasks', loadComponent: () => import('./pages/admin/task-management/task-management').then(m => m.TaskManagement) },
      {
        path: 'tickets',
        children: [
          { path: '', pathMatch: 'full', loadComponent: () => import('./pages/admin/ticket-management/ticket-list/ticket-list').then(m => m.AdminTicketList) },
          {
            path: 'stats',
            loadComponent: () =>
              import('./pages/admin/ticket-management/ticket-stats/ticket-stats').then(m => m.TicketStatsDashboard),
            title: 'Ticket statistics',
          },
          { path: ':id', loadComponent: () => import('./pages/admin/ticket-management/ticket-detail/ticket-detail').then(m => m.AdminTicketDetail) },
        ]
      },
      { path: 'calendar', loadComponent: () => import('./pages/dashboard/calendar/calendar').then(m => m.Calendar), title: 'Calendar' },
      { path: 'github', loadComponent: () => import('./pages/dashboard/github/github').then(m => m.Github), title: 'GitHub' },
      { path: 'evaluations', loadComponent: () => import('./pages/admin/admin-dashboard/admin-dashboard').then(m => m.AdminDashboard) },
      { path: 'skill-stats', loadComponent: () => import('./pages/admin/skill-stats/skill-stats').then(m => m.SkillStats) },
      { path: 'contract-stats', loadComponent: () => import('./pages/admin/contract-stats/contract-stats').then(m => m.ContractStats) },
      { path: 'job-stats', loadComponent: () => import('./pages/admin/freelancia-job-stats/freelancia-job-stats').then(m => m.FreelanciaJobStats), title: 'Job Board Stats' },
      { path: 'skills', loadComponent: () => import('./pages/admin/skill-management/skill-management').then(m => m.AdminSkillManagement) },
      { path: 'reviews', loadComponent: () => import('./pages/admin/review-management/review-management').then(m => m.ReviewManagement) },
      { path: 'achievements', loadComponent: () => import('./pages/admin/achievement-management/achievement-management').then(m => m.AdminAchievementManagement), title: 'Achievements' },
      { path: 'settings', loadComponent: () => import('./pages/admin/admin-dashboard/admin-dashboard').then(m => m.AdminDashboard) },
    ]
  },

  // Fallback
  { path: '**', redirectTo: '' },
];
