import { ChangeDetectorRef, Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ProjectService, Project } from '../../../core/services/project.service';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { PortfolioService, Skill } from '../../../core/services/portfolio.service';

@Component({
  selector: 'app-update-project',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './update-project.html',
  styleUrl: './update-project.scss',
})
export class UpdateProject {
  id!: number;
  isLoading = false;
  isSubmitting = false;
  submitSuccess = false;
  submitError: string | null = null;

  allSkills: Skill[] = [];
  newSkillName = '';
  showAddSkillModal = false;
  newSkillDomain = '';
  skillFormErrors: Record<string, string> = {};
  errorMessage: string | null = null;

  updateProjectForm!: FormGroup;

  constructor(
    private activatedRoute: ActivatedRoute,
    private ps: ProjectService,
    private fb: FormBuilder,
    private router: Router,
    private portfolioService: PortfolioService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.id = Number(this.activatedRoute.snapshot.paramMap.get('id'));

    this.updateProjectForm = this.fb.group({
      id: this.id,
      clientId: [null as number | null],
      title: ['', [Validators.required, Validators.minLength(3)]],
      description: ['', [Validators.required, Validators.minLength(10)]],
      budget: [null, [Validators.required, Validators.min(1)]],
      deadline: ['', [Validators.required]],
      status: ['', [Validators.required]],
      category: ['', [Validators.required]],
      skillIds: [[], Validators.required]
    });
    this.loadSkills();
    this.getProjectById();
  }

  loadSkills() {
    this.portfolioService.getAllSkills().subscribe({
      next: (skills) => {
        this.allSkills = skills;
      },
      error: (err) => {
        console.error('Error loading skills', err);
      }
    });
  }
  
  getProjectById(): void {
    this.isLoading = true;
    this.ps.getProjectById(this.id).subscribe({
      next: (res: Project | null) => {
        if (res) {
          const formattedDate = res.deadline ? String(res.deadline).split('T')[0] : '';
          this.updateProjectForm.patchValue({
            ...res,
            title: res.title ?? '',
            description: res.description ?? '',
            category: res.category ?? '',
            status: res.status ?? '',
            skillIds: res.skills
              ? res.skills.map((s: any) => s.id)
              : [],
            deadline: formattedDate
          });
        }
        this.isLoading = false;
      },
      error: () => {
        this.submitError = 'Failed to load project';
        this.isLoading = false;
      }
    });
  }

  onSkillToggle(skillId: number, event: any) {

    const currentSkills = this.updateProjectForm.value.skillIds as number[];

    if (event.target.checked) {
      this.updateProjectForm.patchValue({
        skillIds: [...currentSkills, skillId]
      });
    } else {
      this.updateProjectForm.patchValue({
        skillIds: currentSkills.filter(id => id !== skillId)
      });
    }

    this.updateProjectForm.get('skillIds')?.updateValueAndValidity();
  }


  openAddSkillModal() {
    this.errorMessage = null;
    this.newSkillName = '';
    this.newSkillDomain = '';
    this.skillFormErrors = {};
    this.showAddSkillModal = true;
  }

  closeAddSkillModal() {
    this.showAddSkillModal = false;
    this.cdr.detectChanges();
  }


  private validateSkillForm(): boolean {
    this.skillFormErrors = {};

    const name = this.newSkillName.trim();
    const domain = this.newSkillDomain.trim();

    if (!name) {
      this.skillFormErrors['name'] = 'Skill name is required.';
    }

    if (!domain) {
      this.skillFormErrors['domain'] = 'Domain is required.';
    }

    return Object.keys(this.skillFormErrors).length === 0;
  }

  addSkill() {
    if (!this.validateSkillForm()) return;

    const skill: Skill = {
      name: this.newSkillName.trim(),
      domain: this.newSkillDomain.trim(),
      description: 'Added from project form',
      userId: 2,  // not needed for project creation
      verified: false,    // default
      score: 0            // default
    };

    this.portfolioService.createSkill(skill).subscribe({
      next: (createdSkill) => {
        this.allSkills.push(createdSkill);

        // auto-select it in project form
        const currentSkills = this.updateProjectForm.value.skillIds || [];

        this.updateProjectForm.patchValue({
          skillIds: [...currentSkills, createdSkill.id!]
        });

        this.closeAddSkillModal();
      },
      error: (err) => {
        console.error('Failed to create skill', err);
        this.errorMessage = 'Failed to create skill.';
      }
    });
  }

  updateProject(): void {

    if (this.updateProjectForm.invalid) {
      this.updateProjectForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;

    const formValue = this.updateProjectForm.value;

    if (formValue.deadline && !formValue.deadline.includes('T')) {
      formValue.deadline = `${formValue.deadline}T00:00:00`;
    }

    this.ps.updateProject(formValue).subscribe({
      next: () => {
        this.submitSuccess = true;
        this.isSubmitting = false;

        setTimeout(() => {
          this.router.navigateByUrl('/dashboard/my-projects');
        }, 1500);
      },
      error: (err) => {
        console.error(err);
        this.submitError = 'Failed to update project';
        this.isSubmitting = false;
      }
    });
  }
}
