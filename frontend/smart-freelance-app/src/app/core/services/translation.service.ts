import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface Language {
  code: string;
  name: string;
  flag: string;
}

interface TranslationResponse {
  translatedText: string;
}

@Injectable({ providedIn: 'root' })
export class TranslationService {

  readonly LANGUAGES: Language[] = [
    { code: 'en', name: 'English',   flag: '🇬🇧' },
    { code: 'fr', name: 'Français',  flag: '🇫🇷' },
    { code: 'ar', name: 'العربية',   flag: '🇹🇳' },
    { code: 'es', name: 'Español',   flag: '🇪🇸' },
    { code: 'de', name: 'Deutsch',   flag: '🇩🇪' },
    { code: 'it', name: 'Italiano',  flag: '🇮🇹' },
    { code: 'pt', name: 'Português', flag: '🇧🇷' },
    { code: 'zh', name: '中文',       flag: '🇨🇳' },
    { code: 'tr', name: 'Türkçe',    flag: '🇹🇷' },
  ];

  /** Backend endpoint for job translations (FreelanciaJob microservice) */
  private readonly JOB_TRANSLATE_URL = `${environment.apiGatewayUrl}/freelancia-job/jobs/translate`;

  /** Backend endpoint for chat message translations (Chat microservice) */
  private readonly CHAT_TRANSLATE_URL = `${environment.apiGatewayUrl}/chat/api/messages/translate`;

  constructor(private http: HttpClient) {}

  /**
   * Translate text via the FreelanciaJob microservice.
   * Used for job title / description in show-job, list-jobs, etc.
   */
  translate(text: string, targetLang: string, sourceLang = 'auto'): Observable<string> {
    return this.callBackend(this.JOB_TRANSLATE_URL, text, targetLang, sourceLang);
  }

  /**
   * Translate a chat message via the Chat microservice.
   */
  translateMessage(text: string, targetLang: string, sourceLang = 'auto'): Observable<string> {
    return this.callBackend(this.CHAT_TRANSLATE_URL, text, targetLang, sourceLang);
  }

  getLangName(code: string): string {
    return this.LANGUAGES.find(l => l.code === code)?.name ?? code;
  }

  getLangFlag(code: string): string {
    return this.LANGUAGES.find(l => l.code === code)?.flag ?? '🌍';
  }

  private callBackend(url: string, text: string, targetLang: string, sourceLang: string): Observable<string> {
    if (!text?.trim()) return of(text);
    return this.http.post<TranslationResponse>(url, { text, targetLang, sourceLang }).pipe(
      map(res => res?.translatedText ?? text),
      catchError(() => of(text))
    );
  }
}
