import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MeetingService } from '../../../core/services/meeting.service';
import { UserService, User } from '../../../core/services/user.service';
import { MeetingType, ProjectDto, ContractDto } from '../../../core/models/meeting.models';

@Component({
  selector: 'app-schedule-meeting',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './schedule-meeting.html',
  styleUrl: './schedule-meeting.scss',
})
export class ScheduleMeeting implements OnInit {
  freelancers = signal<User[]>([]);
  projects = signal<ProjectDto[]>([]);
  contracts = signal<ContractDto[]>([]);
  loading = signal(false);
  submitting = signal(false);
  error = signal<string | null>(null);
  success = signal(false);

  freelancerSearch = '';
  get filteredFreelancers(): User[] {
    const q = this.freelancerSearch.toLowerCase();
    return this.freelancers().filter(u =>
      !q ||
      u.firstName.toLowerCase().includes(q) ||
      u.lastName.toLowerCase().includes(q) ||
      u.email.toLowerCase().includes(q)
    );
  }

  form = {
    freelancerId: null as number | null,
    title: '',
    agenda: '',
    startTime: '',
    endTime: '',
    meetingType: 'VIDEO_CALL' as MeetingType,
    projectId: null as number | null,
    contractId: null as number | null,
  };

  get linkedContractTitle(): string {
    const c = this.contracts().find(c => c.id === this.form.contractId);
    return c ? c.title : `Contract #${this.form.contractId}`;
  }

  meetingTypes: { value: MeetingType; label: string; icon: string }[] = [
    { value: 'VIDEO_CALL', label: 'Video Call', icon: '📹' },
    { value: 'VOICE_CALL', label: 'Voice Call', icon: '📞' },
    { value: 'IN_PERSON', label: 'In Person', icon: '🤝' },
  ];

  constructor(
    private meetingService: MeetingService,
    private userService: UserService,
    private router: Router,
    private route: ActivatedRoute,
  ) {}

  ngOnInit() {
    // Pre-fill from query params (e.g. when coming from contract conflict page)
    const params = this.route.snapshot.queryParamMap;
    const contractId = params.get('contractId');
    const freelancerId = params.get('freelancerId');
    if (contractId) this.form.contractId = Number(contractId);
    if (freelancerId) this.form.freelancerId = Number(freelancerId);

    this.loading.set(true);
    this.userService.getAll().subscribe(users => {
      this.freelancers.set(users.filter(u => u.role === 'FREELANCER' && u.isActive));
      this.loading.set(false);
    });
    this.meetingService.getMyProjects().subscribe(p => this.projects.set(p));
    this.meetingService.getMyContracts().subscribe(c => this.contracts.set(c));
  }

  minStart(): string {
    const now = new Date();
    now.setMinutes(now.getMinutes() + 5);
    return now.toISOString().slice(0, 16);
  }

  submit() {
    this.error.set(null);
    if (!this.form.freelancerId) { this.error.set('Please select a freelancer.'); return; }
    if (!this.form.title.trim()) { this.error.set('Title is required.'); return; }
    if (!this.form.startTime)    { this.error.set('Start time is required.'); return; }
    if (!this.form.endTime)      { this.error.set('End time is required.'); return; }

    const start = new Date(this.form.startTime);
    const end   = new Date(this.form.endTime);
    if (end <= start) { this.error.set('End time must be after start time.'); return; }
    const diffMin = (end.getTime() - start.getTime()) / 60000;
    if (diffMin < 15)  { this.error.set('Meeting must be at least 15 minutes long.'); return; }
    if (diffMin > 480) { this.error.set('Meeting cannot exceed 8 hours.'); return; }

    this.submitting.set(true);
    this.meetingService.create({
      freelancerId: this.form.freelancerId!,
      title: this.form.title.trim(),
      agenda: this.form.agenda.trim() || undefined,
      startTime: new Date(this.form.startTime).toISOString(),
      endTime: new Date(this.form.endTime).toISOString(),
      meetingType: this.form.meetingType,
      projectId: this.form.projectId ?? undefined,
      contractId: this.form.contractId ?? undefined,
    }).subscribe({
      next: meeting => {
        this.submitting.set(false);
        this.router.navigate(['/dashboard/meetings', meeting.id]);
      },
      error: err => {
        this.submitting.set(false);
        this.error.set(err?.error?.message ?? 'Failed to schedule meeting. Please try again.');
      },
    });
  }
}
