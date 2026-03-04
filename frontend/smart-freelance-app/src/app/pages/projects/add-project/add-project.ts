import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ProjectService, Project } from '../../../core/services/project.service';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';
import { Router } from '@angular/router';
import { PortfolioService, Skill } from '../../../core/services/portfolio.service';

@Component({
  selector: 'app-add-project',
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './add-project.html',
  styleUrl: './add-project.scss',
})
export class AddProject implements OnInit {
  postProjectForm!: FormGroup;
  isSubmitting = false;
  submitSuccess = false;
  submitError: string | null = null;
  minDate!: string;

  allSkills: Skill[] = [];
  newSkillName = '';
  showAddSkillModal = false;
  newSkillDomain = '';
  skillFormErrors: Record<string, string> = {};
  errorMessage: string | null = null;

  constructor(
    private projectService: ProjectService,
    private authService: AuthService,
    private userService: UserService,
    private portfolioService: PortfolioService,
    private fb: FormBuilder,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    

    const today = new Date();
    const yyyy = today.getFullYear();
    const mm = String(today.getMonth() + 1).padStart(2, '0');
    const dd = String(today.getDate()).padStart(2, '0');
    this.minDate = `${yyyy}-${mm}-${dd}`;


    this.postProjectForm = this.fb.group({
      title: ['', [Validators.required, Validators.minLength(3)]],
      description: ['', [Validators.required, Validators.minLength(10)]],
      budget: [null, [Validators.required, Validators.min(1)]],
      deadline: ['', [Validators.required, this.futureDateValidator.bind(this)]],
      status: ['', Validators.required],
      category: ['', Validators.required],
      skillIds: [[], Validators.required]   // ← tableau d'IDs
    });
    
    this.loadSkills();
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
        const currentSkills = this.postProjectForm.value.skillIds || [];

        this.postProjectForm.patchValue({
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

  onSkillToggle(skillId: number, event: any) {
    const currentSkills = this.postProjectForm.value.skillIds as number[];

    if (event.target.checked) {
      this.postProjectForm.patchValue({
        skillIds: [...currentSkills, skillId]
      });
    } else {
      this.postProjectForm.patchValue({
        skillIds: currentSkills.filter(id => id !== skillId)
      });
    }

    this.postProjectForm.get('skillIds')?.updateValueAndValidity();
  }

  addNewSkill() {
    if (!this.newSkillName || this.newSkillName.trim().length < 2) {
      return;
    }

    const skillToCreate: Skill = {
      name: this.newSkillName.trim(),
      domain: 'General',
      description: 'Created from project form'
    };

    this.portfolioService.createSkill(skillToCreate).subscribe({
      next: (createdSkill) => {
        this.allSkills.push(createdSkill);

        // Automatically select it
        const currentSkills = this.postProjectForm.value.skillIds;
        this.postProjectForm.patchValue({
          skillIds: [...currentSkills, createdSkill.id]
        });

        this.newSkillName = '';
      },
      error: (err) => {
        console.error('Error creating skill', err);
      }
    });
  }

  futureDateValidator(control: any) {
    if (!control.value) return null; // empty handled by required

    const today = new Date();
    today.setHours(0,0,0,0); // start of today
    const inputDate = new Date(control.value);

    if (inputDate <= today) {
      return { invalidDate: true }; // validation error
    }
    return null;
  }

  

  postProject() {
    if (this.postProjectForm.invalid) {
      this.postProjectForm.markAllAsTouched();
      return;
    }


    
    const formValue = { ...this.postProjectForm.value };


    // Fix deadline: turn "2025-03-08" → "2025-03-08T00:00:00" (or with your timezone if needed)
    if (formValue.deadline) {
      formValue.deadline = `${formValue.deadline}T00:00:00`;   // midnight
      // Alternative (full ISO with Z): formValue.deadline += 'T00:00:00Z';
    }

    this.isSubmitting = true;

    const email = this.authService.getPreferredUsername();
    if (!email) {
      this.submitError = 'You must be signed in to create a project.';
      this.isSubmitting = false;
      return;
    }

    this.userService.getByEmail(email).subscribe({
      next: (user) => {
        if (!user?.id) {
          this.submitError = 'Could not identify your account. Please try again.';
          this.isSubmitting = false;
          return;
        }
        // clientId = user id of the user who created the project (the logged-in CLIENT)
        const projectPayload = { ...formValue, clientId: user.id };
        this.projectService.createProject(projectPayload).subscribe({
          next: (res: Project | null) => {
            this.submitSuccess = true;
            this.isSubmitting = false;
            setTimeout(() => {
              this.router.navigateByUrl('/dashboard/my-projects');
            }, 1800);
          },
          error: (err: { error?: { message?: string } }) => {
            console.error('Create failed', err);
            this.submitError = err?.error?.message || 'Failed to create project. Please try again.';
            this.isSubmitting = false;
          },
        });
      },
      error: () => {
        this.submitError = 'Could not load your profile. Please try again.';
        this.isSubmitting = false;
      },
    });
  }

  onCancel() {
    this.router.navigateByUrl('/dashboard/my-projects');
  }

  // Optional helper – not needed anymore but you can keep it
  clearFormProject() {
    this.postProjectForm.reset();
  }
}
