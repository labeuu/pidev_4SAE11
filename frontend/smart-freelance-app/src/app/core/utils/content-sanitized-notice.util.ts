import { ToastService } from '../services/toast.service';

/**
 * Backend may censor profanity (PurgoMalum). Toast only supports one message at a time,
 * so we append a short note to success when stored text differs from what the user sent.
 */
export function toastSuccessWithOptionalFilterNote(
  toast: ToastService,
  textBeforeRequest: string,
  textAfterSave: string,
  successMessage: string
): void {
  const a = (textBeforeRequest ?? '').trim();
  const b = (textAfterSave ?? '').trim();
  const filtered = a.length > 0 && a !== b;
  toast.success(
    filtered ? `${successMessage} Some wording was filtered before saving.` : successMessage
  );
}
