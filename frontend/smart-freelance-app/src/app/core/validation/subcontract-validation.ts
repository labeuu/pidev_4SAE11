/** Limites alignées avec les contraintes backend (Subcontracting) + UX. */

export const SC_TITLE_MIN = 3;
export const SC_TITLE_MAX = 200;
export const SC_SCOPE_MAX = 5000;
/** Si le périmètre est renseigné, longueur minimale (évite un seul mot vague). */
export const SC_SCOPE_MIN_WHEN_SET = 5;
export const SC_BUDGET_MAX = 999_999_999;
export const SC_SKILL_MAX_LEN = 80;
export const SC_MAX_SKILLS = 30;

export const DELIV_TITLE_MIN = 2;
export const DELIV_TITLE_MAX = 200;
export const DELIV_DESC_MAX = 2000;

export const SUBMIT_URL_MAX = 2048;
export const SUBMIT_NOTE_MAX = 2000;

export const REASON_MIN = 3;
export const REASON_MAX = 500;

export const HISTORY_FILTER_MAX = 200;

function isValidHttpUrl(raw: string): boolean {
  const s = raw.trim();
  if (!s) return false;
  try {
    const u = new URL(s);
    return u.protocol === 'http:' || u.protocol === 'https:';
  } catch {
    return false;
  }
}

export interface SubcontractCreateValidationInput {
  title: string | undefined;
  projectId: number | undefined;
  offerId: number | undefined;
  subcontractorId: number | undefined;
  category: string | undefined;
  budget: number | string | null | undefined;
  scope: string | undefined;
  requiredSkills: string[];
  startDate: string | undefined;
  deadline: string | undefined;
}

export interface SubcontractCreateValidationOptions {
  /** Exige un budget &gt; 0 (formulaire création sous-traitance). Défaut : true. */
  requireBudget?: boolean;
}

export function validateSubcontractCreateForm(
  input: SubcontractCreateValidationInput,
  options: SubcontractCreateValidationOptions = {}
): { valid: boolean; errors: Record<string, string> } {
  const requireBudget = options.requireBudget !== false;
  const errors: Record<string, string> = {};

  const title = (input.title ?? '').trim();
  if (!title) errors['title'] = 'Le titre est obligatoire.';
  else if (title.length < SC_TITLE_MIN) {
    errors['title'] = `Le titre doit contenir au moins ${SC_TITLE_MIN} caractères.`;
  } else if (title.length > SC_TITLE_MAX) {
    errors['title'] = `Le titre ne peut pas dépasser ${SC_TITLE_MAX} caractères.`;
  }

  const pid = input.projectId !== undefined && input.projectId !== null ? Number(input.projectId) : NaN;
  const oid = input.offerId !== undefined && input.offerId !== null ? Number(input.offerId) : NaN;
  const hasP = Number.isFinite(pid) && pid > 0;
  const hasO = Number.isFinite(oid) && oid > 0;
  if (!hasP && !hasO) {
    errors['mission'] = 'Sélectionnez une mission (projet ou offre) dans la liste.';
  } else if (hasP && hasO) {
    errors['mission'] = 'Une seule mission à la fois (projet ou offre).';
  }

  const sid = Number(input.subcontractorId);
  if (!Number.isFinite(sid) || sid <= 0) {
    errors['subcontractorId'] = 'Sélectionnez un sous-traitant via le matching IA.';
  }

  const cat = (input.category ?? '').trim();
  if (!cat) errors['category'] = 'La catégorie est obligatoire.';

  const budgetRaw = input.budget;
  const budgetEmpty =
    budgetRaw === undefined ||
    budgetRaw === null ||
    (typeof budgetRaw === 'string' && budgetRaw.trim() === '') ||
    (typeof budgetRaw === 'number' && Number.isNaN(budgetRaw));

  if (requireBudget && budgetEmpty) {
    errors['budget'] = 'Indiquez un budget en TND (montant strictement positif).';
  } else if (!budgetEmpty) {
    const b = typeof budgetRaw === 'string' ? Number(String(budgetRaw).replace(',', '.').trim()) : Number(budgetRaw);
    if (Number.isNaN(b)) errors['budget'] = 'Le budget doit être un nombre valide.';
    else if (b <= 0) errors['budget'] = 'Le budget doit être strictement positif.';
    else if (b > SC_BUDGET_MAX) errors['budget'] = 'Le budget dépasse la valeur autorisée.';
  }

  const scope = (input.scope ?? '').trim();
  if (scope.length > SC_SCOPE_MAX) {
    errors['scope'] = `Le périmètre ne peut pas dépasser ${SC_SCOPE_MAX} caractères.`;
  } else if (scope.length > 0 && scope.length < SC_SCOPE_MIN_WHEN_SET) {
    errors['scope'] = `Le périmètre doit contenir au moins ${SC_SCOPE_MIN_WHEN_SET} caractères ou rester vide.`;
  }

  const skills = input.requiredSkills ?? [];
  if (skills.length < 1) {
    errors['requiredSkills'] = 'Ajoutez au moins une compétence requise.';
  } else if (skills.length > SC_MAX_SKILLS) {
    errors['requiredSkills'] = `Maximum ${SC_MAX_SKILLS} compétences. Retirez ou fusionnez des entrées.`;
  } else {
    for (const raw of skills) {
      const s = (raw ?? '').trim();
      if (!s) {
        errors['requiredSkills'] = 'Chaque compétence doit contenir au moins un caractère (supprimez les entrées vides).';
        break;
      }
      if (s.length > SC_SKILL_MAX_LEN) {
        errors['requiredSkills'] = `Chaque compétence est limitée à ${SC_SKILL_MAX_LEN} caractères.`;
        break;
      }
    }
  }

  const sd = (input.startDate ?? '').trim();
  const dl = (input.deadline ?? '').trim();
  if (sd && dl) {
    const t1 = Date.parse(sd);
    const t2 = Date.parse(dl);
    if (!Number.isNaN(t1) && !Number.isNaN(t2) && t1 > t2) {
      errors['dates'] = 'La date de fin doit être le même jour ou après la date de début.';
    }
  }

  return { valid: Object.keys(errors).length === 0, errors };
}

export function validateDeliverableModal(input: {
  title: string | undefined;
  description: string | undefined;
  deadline: string | undefined;
}): { valid: boolean; errors: Record<string, string> } {
  const errors: Record<string, string> = {};
  const title = (input.title ?? '').trim();
  if (!title) errors['title'] = 'Le titre du livrable est obligatoire.';
  else if (title.length < DELIV_TITLE_MIN) {
    errors['title'] = `Le titre doit contenir au moins ${DELIV_TITLE_MIN} caractères.`;
  } else if (title.length > DELIV_TITLE_MAX) {
    errors['title'] = `Le titre ne peut pas dépasser ${DELIV_TITLE_MAX} caractères.`;
  }

  const desc = (input.description ?? '').trim();
  if (desc.length > DELIV_DESC_MAX) {
    errors['description'] = `La description ne peut pas dépasser ${DELIV_DESC_MAX} caractères.`;
  }

  return { valid: Object.keys(errors).length === 0, errors };
}

export function validateDeliverableSubmission(
  submissionUrl: string,
  submissionNote: string
): { valid: boolean; errors: Record<string, string> } {
  const errors: Record<string, string> = {};
  const url = (submissionUrl ?? '').trim();
  if (!url) errors['submissionUrl'] = 'Indiquez une URL de livrable (http ou https).';
  else if (url.length > SUBMIT_URL_MAX) errors['submissionUrl'] = 'URL trop longue.';
  else if (!isValidHttpUrl(url)) {
    errors['submissionUrl'] = 'URL invalide : utilisez une adresse commençant par http:// ou https://';
  }

  const note = submissionNote ?? '';
  if (note.length > SUBMIT_NOTE_MAX) {
    errors['submissionNote'] = `La note ne peut pas dépasser ${SUBMIT_NOTE_MAX} caractères.`;
  }

  return { valid: Object.keys(errors).length === 0, errors };
}

export function validatePromptReason(reason: string | null | undefined): string | null {
  if (reason == null) return null;
  const t = reason.trim();
  if (t.length < REASON_MIN) {
    return `La raison doit contenir au moins ${REASON_MIN} caractères.`;
  }
  if (t.length > REASON_MAX) {
    return `La raison ne peut pas dépasser ${REASON_MAX} caractères.`;
  }
  return null;
}
