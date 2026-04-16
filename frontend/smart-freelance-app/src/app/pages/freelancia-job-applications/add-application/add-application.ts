import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { JobApplicationService } from '../../../core/services/job-application.service';
import { JobService, Job, FitScoreResult } from '../../../core/services/job.service';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';
import { asyncScheduler } from 'rxjs';
import { observeOn } from 'rxjs/operators';

// ── Types ─────────────────────────────────────────────────────────────────────

interface FilePreview {
  file: File;
  name: string;
  size: number;
  type: string;
  isImage: boolean;
  dataUrl?: string; // base64 for image thumbnails
}

const ALLOWED_MIME = new Set([
  'application/pdf',
  'image/png',
  'image/jpeg',
  'application/msword',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
]);

const MAX_FILES     = 5;
const MAX_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB

@Component({
  selector: 'app-add-application',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './add-application.html',
  styleUrl: './add-application.scss',
})
export class AddApplication implements OnInit {

  // ── Form & routing state ──────────────────────────────────────────────────
  form!: FormGroup;
  job: Job | null = null;
  isLoading = true;
  isSubmitting = false;
  submitSuccess = false;
  submitError: string | null = null;
  userId: number | null = null;
  jobId!: number;

  // ── File upload state ─────────────────────────────────────────────────────
  filePreviews: FilePreview[] = [];
  isDragging = false;
  uploadProgress = 0;
  fileError: string | null = null;

  /** AI profile vs job skills (same API as job details page). */
  fitScore: FitScoreResult | null = null;
  isScoringLoading = false;
  scoringError: string | null = null;

  constructor(
    private fb:          FormBuilder,
    private route:       ActivatedRoute,
    private router:      Router,
    private appService:  JobApplicationService,
    private jobService:  JobService,
    private authService: AuthService,
    private userService: UserService,
  ) {}

  ngOnInit(): void {
    this.jobId = Number(this.route.snapshot.paramMap.get('id'));

    this.form = this.fb.group({
      proposalMessage:  ['', [Validators.required, Validators.minLength(20)]],
      expectedRate:     [null, [Validators.min(0)]],
      availabilityStart: [''],
    });

    this.jobService.getById(this.jobId).pipe(observeOn(asyncScheduler)).subscribe({
      next:  job => { this.job = job; this.isLoading = false; },
      error: ()  => { this.isLoading = false; },
    });

    const email = this.authService.getPreferredUsername();
    if (email) {
      this.userService.getByEmail(email).subscribe({
        next: user => (this.userId = user?.id ?? null),
      });
    }
  }

  // ── Drag & drop ───────────────────────────────────────────────────────────

  onDragOver(e: DragEvent): void {
    e.preventDefault();
    e.stopPropagation();
    this.isDragging = true;
  }

  onDragLeave(e: DragEvent): void {
    e.preventDefault();
    this.isDragging = false;
  }

  onDrop(e: DragEvent): void {
    e.preventDefault();
    this.isDragging = false;
    this.addFiles(Array.from(e.dataTransfer?.files ?? []));
  }

  onFileSelected(e: Event): void {
    const input = e.target as HTMLInputElement;
    this.addFiles(Array.from(input.files ?? []));
    input.value = ''; // allow re-selecting the same file
  }

  // ── File management ───────────────────────────────────────────────────────

  private addFiles(incoming: File[]): void {
    this.fileError = null;
    const wouldTotal = this.filePreviews.length + incoming.length;
    if (wouldTotal > MAX_FILES) {
      this.fileError = `Maximum ${MAX_FILES} files allowed (you currently have ${this.filePreviews.length}).`;
      return;
    }
    for (const file of incoming) {
      if (file.size > MAX_SIZE_BYTES) {
        this.fileError = `"${file.name}" exceeds the 10 MB size limit.`;
        return;
      }
      if (!ALLOWED_MIME.has(file.type)) {
        this.fileError = `"${file.name}" is not an allowed file type. Use PDF, PNG, JPG, DOC or DOCX.`;
        return;
      }
      const preview: FilePreview = {
        file, name: file.name, size: file.size, type: file.type,
        isImage: file.type.startsWith('image/'),
      };
      if (preview.isImage) {
        const reader = new FileReader();
        reader.onload = ev => (preview.dataUrl = ev.target?.result as string);
        reader.readAsDataURL(file);
      }
      this.filePreviews.push(preview);
    }
  }

  removeFile(index: number): void {
    this.filePreviews.splice(index, 1);
    this.fileError = null;
  }

  // ── Submit ────────────────────────────────────────────────────────────────

  onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    if (!this.userId)      { this.submitError = 'User not identified. Please re-login.'; return; }

    this.isSubmitting  = true;
    this.submitError   = null;
    this.uploadProgress = 0;

    const raw = this.form.value;
    const fd  = new FormData();
    fd.append('freelancerId',     String(this.userId));
    fd.append('proposalMessage',  raw.proposalMessage);
    if (raw.expectedRate)     fd.append('expectedRate',     String(raw.expectedRate));
    if (raw.availabilityStart) fd.append('availabilityStart', raw.availabilityStart);
    this.filePreviews.forEach(p => fd.append('files', p.file));

    this.appService
      .applyToJob(this.jobId, fd, pct => (this.uploadProgress = pct))
      .subscribe({
        next: result => {
          this.isSubmitting = false;
          if (result) {
            this.uploadProgress = 100;
            this.submitSuccess  = true;
            setTimeout(() => this.router.navigate(['/dashboard/my-job-applications']), 1500);
          } else {
            this.submitError = 'Failed to submit application. You may have already applied to this job.';
          }
        },
        error: (error: HttpErrorResponse) => {
          this.isSubmitting = false;
          if (error.status === 409) {
            this.submitError = 'You have already applied to this job.';
            return;
          }
          if (error.status === 400) {
            this.submitError = 'Please check your proposal and optional fields, then try again.';
            return;
          }
          if (error.status === 404) {
            this.submitError = 'This job was not found or is no longer available.';
            return;
          }
          this.submitError = 'Failed to submit application. Please try again.';
        },
      });
  }

  // ── Template helpers ──────────────────────────────────────────────────────

  f(name: string) { return this.form.get(name); }

  fileIcon(type: string): string {
    if (type === 'application/pdf')     return 'bi-file-earmark-pdf text-danger';
    if (type.includes('word'))          return 'bi-file-earmark-word text-primary';
    return 'bi-file-earmark text-secondary';
  }

  formatSize(bytes: number): string {
    if (bytes < 1_024)        return `${bytes} B`;
    if (bytes < 1_048_576)    return `${(bytes / 1_024).toFixed(1)} KB`;
    return `${(bytes / 1_048_576).toFixed(1)} MB`;
  }

  tierLabel(tier: string): string {
    return tier?.replaceAll('_', ' ') ?? '';
  }

  analyzeMyFit(): void {
    if (!this.job?.id || !this.userId || this.isScoringLoading) return;
    this.isScoringLoading = true;
    this.scoringError = null;
    this.fitScore = null;

    this.jobService.getFitScore(this.job.id, this.userId).subscribe({
      next: result => {
        this.fitScore = result;
        this.isScoringLoading = false;
        if (!result) {
          this.scoringError = 'Could not analyse your profile. Please try again.';
        }
      },
      error: () => {
        this.isScoringLoading = false;
        this.scoringError = 'Could not analyse your profile. Please try again.';
      },
    });
  }
}
