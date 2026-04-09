import { Injectable } from '@angular/core';

/** User-chosen AI stack for Task AI features (persisted in localStorage). */
export type AiEngineId = 'ollama';

const DEFAULT_ENGINE: AiEngineId = 'ollama';

@Injectable({ providedIn: 'root' })
export class AiEnginePreferenceService {
  getEngine(): AiEngineId {
    return DEFAULT_ENGINE;
  }

  setEngine(engine: AiEngineId): void {
    // Single backend now; keep method for compatibility with existing callers.
    void engine;
  }
}
