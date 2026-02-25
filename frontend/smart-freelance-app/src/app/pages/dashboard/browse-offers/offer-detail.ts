import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { UserService, User } from '../../../core/services/user.service';
import {
  OfferService,
  Offer,
  OfferApplicationRequest,
  OfferQuestionResponse,
} from '../../../core/services/offer.service';

@Component({
  selector: 'app-offer-detail',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterLink],
  templateUrl: './offer-detail.html',
  styleUrl: './offer-detail.scss',
})
export class OfferDetail implements OnInit {
  offer: Offer | null = null;
  loading = true;
  applyModalOpen = false;
  form: FormGroup;
  submitting = false;
  submitError: string | null = null;
  currentUser: User | null = null;

  questions: OfferQuestionResponse[] = [];
  loadingQuestions = false;
  questionText = '';
  submittingQuestion = false;
  questionError: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private auth: AuthService,
    private userService: UserService,
    private offerService: OfferService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef
  ) {
    this.form = this.fb.group({
      message: ['', [Validators.required, Validators.minLength(20), Validators.maxLength(2000)]],
      proposedBudget: [null, [Validators.required, Validators.min(0.01)]],
      estimatedDuration: [null, [Validators.min(1)]],
      portfolioUrl: [''],
    });
  }

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    const email = this.auth.getPreferredUsername();
    if (!email) {
      this.loading = false;
      this.cdr.detectChanges();
      return;
    }
    this.userService.getByEmail(email).subscribe((u) => {
      this.currentUser = u ?? null;
      this.offerService.getOfferById(id).subscribe((offer) => {
        this.offer = offer ?? null;
        this.loading = false;
        if (this.currentUser?.id && offer?.id) {
          this.offerService.recordOfferView(this.currentUser.id, offer.id).subscribe();
        }
        if (offer?.id) this.loadQuestions(offer.id);
        this.cdr.detectChanges();
      });
    });
  }

  loadQuestions(offerId: number): void {
    this.loadingQuestions = true;
    this.offerService.getOfferQuestions(offerId).subscribe((list) => {
      this.questions = list ?? [];
      this.loadingQuestions = false;
      this.cdr.detectChanges();
    });
  }

  submitQuestion(): void {
    if (!this.offer?.id || !this.currentUser?.id || !this.questionText?.trim() || this.questionText.trim().length < 10) return;
    this.submittingQuestion = true;
    this.questionError = null;
    this.offerService.addOfferQuestion(this.offer.id, this.currentUser.id, this.questionText.trim()).subscribe({
      next: (q) => {
        this.submittingQuestion = false;
        if (q) {
          this.questions = [q, ...this.questions];
          this.questionText = '';
        } else this.questionError = 'Failed to send question.';
        this.cdr.detectChanges();
      },
      error: () => {
        this.submittingQuestion = false;
        this.questionError = 'Failed to send question.';
        this.cdr.detectChanges();
      },
    });
  }

  formatDate(s: string | null | undefined): string {
    if (!s) return '';
    const d = new Date(s);
    return d.toLocaleDateString(undefined, { dateStyle: 'short' });
  }

  openApplyModal(): void {
    this.submitError = null;
    this.form.reset({ message: '', proposedBudget: null, estimatedDuration: null, portfolioUrl: '' });
    this.applyModalOpen = true;
  }

  closeApplyModal(): void {
    if (!this.submitting) this.applyModalOpen = false;
  }

  submitApplication(): void {
    if (!this.offer || !this.currentUser?.id || this.form.invalid) return;
    this.form.markAllAsTouched();
    this.submitting = true;
    this.submitError = null;
    const v = this.form.value;
    const req: OfferApplicationRequest = {
      offerId: this.offer.id,
      clientId: this.currentUser.id,
      message: (v.message as string).trim(),
      proposedBudget: Number(v.proposedBudget),
      estimatedDuration: v.estimatedDuration ? Number(v.estimatedDuration) : undefined,
      portfolioUrl: (v.portfolioUrl as string)?.trim() || undefined,
    };
    this.offerService.applyToOffer(req).subscribe({
      next: () => {
        this.submitting = false;
        this.applyModalOpen = false;
        this.cdr.detectChanges();
      },
      error: (err: unknown) => {
        this.submitting = false;
        const e = err as { error?: { message?: string }; message?: string };
        this.submitError = e?.error?.message || e?.message || 'Failed to submit application.';
        this.cdr.detectChanges();
      },
    });
  }
}
