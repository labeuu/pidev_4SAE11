import { Component, OnInit, ChangeDetectorRef, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Subscription, TimeoutError, forkJoin } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { PortfolioService, Skill, EvaluationTest, EvaluationResult, Domain, DOMAIN_LABELS } from '../../../core/services/portfolio.service';
import { FormsModule } from '@angular/forms';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-skill-management',
  imports: [CommonModule, FormsModule],
  templateUrl: './skill-management.html',
  styleUrl: './skill-management.scss',
  standalone: true,
})
export class SkillManagement implements OnInit, OnDestroy {
  /** Tests de vérification générés par le backend IA — masqués si false */
  readonly showAiUi = environment.showAiUi;

  skills: Skill[] = [];
  showAddModal = false;
  showDomainDropdown = false;
  newSkillName = '';

  // Domain checkbox state — pre-seeded from the local DOMAIN_LABELS map so
  // checkboxes render immediately; the backend call in ngOnInit keeps it in sync.
  availableDomains: Domain[] = Object.keys(DOMAIN_LABELS) as Domain[];
  selectedDomains = new Set<Domain>();
  readonly domainLabels = DOMAIN_LABELS;

  // Test State
  showTestModal = false;
  currentTest: EvaluationTest | null = null;
  currentQuestionIndex = 0;
  selectedOption = '';
  testAnswers: { questionIndex: number; selectedOption: string }[] = [];
  testResult: any = null;
  isGeneratingTest = false;
  isAnswerSubmitted = false;
  isAnswerCorrect = false;

  // History State
  showHistoryModal = false;
  evaluationHistory: EvaluationResult[] = [];
  isLoadingHistory = false;

  // Delete Confirmation State
  showDeleteConfirm = false;
  skillToDeleteId: number | null = null;

  // Error State
  errorMessage: string | null = null;
  /** Erreur API affichée dans la modale (les erreurs globales en haut de page sont masquées par la modale). */
  addModalApiError: string | null = null;
  isAddingSkills = false;
  skillFormErrors: Record<string, string> = {};

  // Subscription management
  private skillsSubscription?: Subscription;

  constructor(
    public auth: AuthService,
    private portfolioService: PortfolioService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.loadSkills();

    this.skillsSubscription = this.portfolioService.skillsUpdated$.subscribe(() => {
      setTimeout(() => this.loadSkills(), 0);
    });
  }

  ngOnDestroy() {
    this.skillsSubscription?.unsubscribe();
  }

  loadSkills() {
    const userId = this.auth.getUserId() || 1;
    this.portfolioService.getUserSkills(userId).subscribe({
      next: (skills) => {
        this.skills = skills;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error loading skills:', err);
        this.errorMessage = 'Failed to load skills.';
        this.cdr.detectChanges();
      }
    });
  }

  // ── Domain checkbox helpers ─────────────────────────────────

  toggleDomain(domain: Domain) {
    if (this.selectedDomains.has(domain)) {
      this.selectedDomains.delete(domain);
    } else {
      this.selectedDomains.add(domain);
    }
    if (this.skillFormErrors['domains']) this.skillFormErrors['domains'] = '';
  }

  isDomainSelected(domain: Domain): boolean {
    return this.selectedDomains.has(domain);
  }

  // ── Grouping ────────────────────────────────────────────────

  get groupedSkills(): { [domain: string]: Skill[] } {
    return this.skills.reduce((acc, skill) => {
      const primary = (skill.domains && skill.domains.length > 0) ? skill.domains[0] : 'OTHER';
      if (!acc[primary]) acc[primary] = [];
      acc[primary].push(skill);
      return acc;
    }, {} as { [domain: string]: Skill[] });
  }

  get skillDomains(): string[] {
    return Object.keys(this.groupedSkills).sort();
  }

  getSkillsByDomain(domain: string): Skill[] {
    return this.groupedSkills[domain] || [];
  }

  // ── Add Modal ───────────────────────────────────────────────

  getSelectedDomainsArray(): Domain[] {
    return [...this.selectedDomains];
  }

  openAddModal() {
    this.errorMessage = null;
    this.addModalApiError = null;
    this.newSkillName = '';
    this.selectedDomains = new Set();
    this.skillFormErrors = {};
    this.showDomainDropdown = false;
    this.showAddModal = true;
  }

  /** Après découpe par , ou ; chaque partie est validée (pas de virgule dans une partie). */
  private static readonly NAME_PART_PATTERN = /^[a-zA-Z0-9 .+#\-\/()_]+$/;

  private parseSkillNames(raw: string): string[] {
    return raw
      .split(/[,;]+/)
      .map((s) => s.trim())
      .filter((s) => s.length > 0);
  }

  private validateSkillForm(): boolean {
    this.skillFormErrors = {};
    const raw = this.newSkillName.trim();
    const parts = this.parseSkillNames(raw);

    if (parts.length === 0) {
      this.skillFormErrors['name'] = 'Skill name is required.';
    } else {
      for (const name of parts) {
        if (name.length < 2) {
          this.skillFormErrors['name'] = `Each skill must be at least 2 characters (check: "${name}").`;
          break;
        }
        if (name.length > 50) {
          this.skillFormErrors['name'] = `Each skill must be 50 characters or less (check: "${name.slice(0, 20)}…").`;
          break;
        }
        if (!SkillManagement.NAME_PART_PATTERN.test(name)) {
          this.skillFormErrors['name'] =
            'Use letters, numbers, spaces, comma between skills, and . + # - / ( ) _ only.';
          break;
        }
      }
    }

    if (this.selectedDomains.size === 0) {
      this.skillFormErrors['domains'] = 'Please select at least one domain.';
    }

    return Object.keys(this.skillFormErrors).length === 0;
  }

  addSkill() {
    this.addModalApiError = null;
    this.showDomainDropdown = false;

    if (!this.validateSkillForm()) {
      this.cdr.detectChanges();
      return;
    }

    const parts = this.parseSkillNames(this.newSkillName.trim());
    for (const name of parts) {
      const taken = this.skills.some((s) => s.name.toLowerCase() === name.toLowerCase());
      if (taken) {
        this.skillFormErrors['name'] = `"${name}" is already in your profile.`;
        this.cdr.detectChanges();
        return;
      }
    }

    const userId = this.auth.getUserId() || 1;
    const domains = [...this.selectedDomains];
    const creations = parts.map((name) =>
      this.portfolioService.createSkill({
        name,
        domains,
        description: 'Added via Dashboard',
        userId
      })
    );

    this.isAddingSkills = true;
    forkJoin(creations).subscribe({
      next: () => {
        this.isAddingSkills = false;
        this.portfolioService.notifySkillsUpdated();
        this.showAddModal = false;
        this.newSkillName = '';
        this.selectedDomains = new Set();
        this.errorMessage = null;
        this.addModalApiError = null;
        this.router.navigate(['/dashboard/my-portfolio']);
        this.cdr.detectChanges();
      },
      error: (err: { error?: unknown; message?: string; status?: number }) => {
        this.isAddingSkills = false;
        console.error('Error adding skill:', err);
        const e = err.error;
        let apiMsg = 'Échec de l’ajout des compétences. Réessayez.';
        if (typeof e === 'string') apiMsg = e;
        else if (e && typeof e === 'object' && 'message' in e) apiMsg = String((e as { message: string }).message);
        else if (err.message) apiMsg = err.message;
        if (err.status === 401 || err.status === 403) {
          apiMsg = 'Session expirée ou accès refusé. Reconnectez-vous et réessayez.';
        }
        this.addModalApiError = apiMsg;
        this.errorMessage = apiMsg;
        setTimeout(() => {
          this.errorMessage = null;
        }, 8000);
        this.cdr.detectChanges();
      }
    });
  }

  // ── Delete ──────────────────────────────────────────────────

  confirmDelete(id: number) {
    this.skillToDeleteId = id;
    this.showDeleteConfirm = true;
  }

  cancelDelete() {
    this.showDeleteConfirm = false;
    this.skillToDeleteId = null;
  }

  deleteSkill() {
    if (this.skillToDeleteId) {
      const userId = this.auth.getUserId() || 1;
      this.portfolioService.deleteUserSkill(userId, this.skillToDeleteId).subscribe({
        next: () => {
          this.loadSkills();
          this.cancelDelete();
        },
        error: (err) => {
          console.error(err);
          this.errorMessage = 'Failed to delete skill.';
          this.cancelDelete();
        }
      });
    }
  }

  // ── Verify / Test ───────────────────────────────────────────

  verifySkill(skillId: number) {
    this.isGeneratingTest = true;
    this.showTestModal = true;
    this.currentTest = null;
    this.testResult = null;
    this.testAnswers = [];
    this.resetQuestionState();
    this.errorMessage = null;

    this.portfolioService.generateTest(skillId).subscribe({
      next: (test) => {
        this.isGeneratingTest = false;
        this.currentTest = test;
        this.currentQuestionIndex = 0;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.isGeneratingTest = false;
        this.showTestModal = false;
        console.error('Error generating test:', err);

        if (err instanceof TimeoutError) {
          this.errorMessage = 'Request timed out after 2 minutes. Please try again.';
        } else if (err.status === 0) {
          this.errorMessage = 'Cannot connect to the Portfolio service (port 8086).';
        } else if (err.status === 404) {
          this.errorMessage = 'Skill not found. Please refresh the page.';
        } else if (err.status === 500) {
          this.errorMessage = 'AI service error. Check the API_KEY environment variable.';
        } else if (err.status === 504) {
          this.errorMessage = 'Gateway timeout. The AI service is taking too long.';
        } else {
          this.errorMessage = err.error?.message || 'Failed to generate test. Please try again later.';
        }
        setTimeout(() => this.errorMessage = null, 10000);
      }
    });
  }

  resetQuestionState() {
    this.selectedOption = '';
    this.isAnswerSubmitted = false;
    this.isAnswerCorrect = false;
  }

  submitAnswer() {
    if (!this.selectedOption || !this.currentTest) return;
    const q = this.currentTest.questions[this.currentQuestionIndex];
    this.selectedOption = this.selectedOption.substring(0, 8);
    this.isAnswerCorrect = this.selectedOption === q.correctOption;
    this.isAnswerSubmitted = true;
    this.testAnswers.push({ questionIndex: this.currentQuestionIndex, selectedOption: this.selectedOption });
  }

  nextQuestion() {
    if (this.currentTest && this.currentQuestionIndex < this.currentTest.questions.length - 1) {
      this.currentQuestionIndex++;
      this.resetQuestionState();
    } else {
      this.finishTest();
    }
  }

  finishTest() {
    if (!this.currentTest) return;
    const userId = this.auth.getUserId() || 1;
    this.portfolioService.submitTest({
      testId: this.currentTest.id,
      freelancerId: userId,
      answers: this.testAnswers
    }).subscribe({
      next: (result) => {
        this.testResult = result;
        this.loadSkills();
      },
      error: (err) => {
        console.error('Error submitting test:', err);
        this.errorMessage = 'Failed to submit test. Please try again.';
        setTimeout(() => this.errorMessage = null, 5000);
      }
    });
  }

  closeTestModal() {
    this.showTestModal = false;
    this.currentTest = null;
    this.testResult = null;
  }

  // ── History ─────────────────────────────────────────────────

  openHistoryModal() {
    this.showHistoryModal = true;
    this.isLoadingHistory = true;
    const userId = this.auth.getUserId() || 1;
    this.portfolioService.getUserEvaluations(userId).subscribe({
      next: (evaluations) => {
        this.evaluationHistory = evaluations.sort((a, b) =>
          new Date(b.evaluatedAt || '').getTime() - new Date(a.evaluatedAt || '').getTime()
        );
        this.isLoadingHistory = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error loading history:', err);
        this.isLoadingHistory = false;
        this.evaluationHistory = [];
        this.cdr.detectChanges();
      }
    });
  }

  closeHistoryModal() {
    this.showHistoryModal = false;
    this.evaluationHistory = [];
  }

  goToHistory() {
    this.closeTestModal();
    this.openHistoryModal();
  }

  formatDate(dateStr?: string): string {
    if (!dateStr) return '—';
    return new Date(dateStr).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  getSkillColor(name: string): number {
    return ((name ? name.charCodeAt(0) : 0) % 6) + 1;
  }
}
