import { Component } from '@angular/core';
import { AuthService } from '../../../core/services/auth.service';
import { CommonModule } from '@angular/common';
import { ClientHome } from '../client-home/client-home';
import { FreelancerHome } from '../freelancer-home/freelancer-home';

/**
 * Role-based dashboard landing: renders ClientHome for clients,
 * FreelancerHome for freelancers.
 */
@Component({
  selector: 'app-dashboard-home',
  imports: [CommonModule, ClientHome, FreelancerHome],
  templateUrl: './dashboard-home.html',
  styleUrl: './dashboard-home.scss',
  standalone: true,
})
export class DashboardHome {
  constructor(public auth: AuthService) {}
}
