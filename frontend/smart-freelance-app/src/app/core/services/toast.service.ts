import { Injectable, signal } from '@angular/core';

export type ToastVariant = 'success' | 'error' | 'info';

@Injectable({ providedIn: 'root' })
export class ToastService {
  readonly message = signal<string | null>(null);
  readonly variant = signal<ToastVariant>('info');

  private timer?: ReturnType<typeof setTimeout>;
  private readonly defaultDurationMs = 4500;
  private readonly errorDurationMs = 6500;

  /**
   * Affiche un toast en bas à droite, fermeture auto ou au clic.
   * Double requestAnimationFrame defers past the current CD + dev-mode check
   * (often scheduled with rAF), avoiding NG0100 when page stats change in the same action.
   */
  show(text: string, variant: ToastVariant = 'info', durationMs?: number): void {
    clearTimeout(this.timer);
    const ms =
      durationMs ??
      (variant === 'error' ? this.errorDurationMs : this.defaultDurationMs);
    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        this.variant.set(variant);
        this.message.set(text);
        this.timer = setTimeout(() => this.dismiss(), ms);
      });
    });
  }

  success(text: string, durationMs?: number): void {
    this.show(text, 'success', durationMs);
  }

  error(text: string, durationMs?: number): void {
    this.show(text, 'error', durationMs);
  }

  info(text: string, durationMs?: number): void {
    this.show(text, 'info', durationMs);
  }

  dismiss(): void {
    clearTimeout(this.timer);
    requestAnimationFrame(() => {
      requestAnimationFrame(() => this.message.set(null));
    });
  }
}
