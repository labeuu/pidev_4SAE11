import { Injectable } from '@angular/core';
import { from, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ElevenLabsService {
  private readonly sttUrl = 'https://api.elevenlabs.io/v1/speech-to-text';

  /**
   * Uses native fetch (NOT Angular HttpClient) to bypass the auth interceptor
   * which injects Authorization: Bearer <JWT> — a header ElevenLabs rejects.
   */
  transcribe(audioBlob: Blob): Observable<string> {
    if (!environment.elevenLabsApiKey?.trim()) {
      return from(Promise.reject(new Error('ElevenLabs is not configured (set elevenLabsApiKey at build time).')));
    }
    // ElevenLabs expects the field name "file", not "audio"
    const mimeType = audioBlob.type || 'audio/webm';
    let ext = 'webm';
    if (mimeType.includes('mp4')) ext = 'mp4';
    else if (mimeType.includes('ogg')) ext = 'ogg';
    const file = new File([audioBlob], `recording.${ext}`, { type: mimeType });

    const formData = new FormData();
    formData.append('file', file);
    formData.append('model_id', 'scribe_v1');

    const promise = fetch(this.sttUrl, {
      method: 'POST',
      headers: {
        'xi-api-key': environment.elevenLabsApiKey,
        // Do NOT set Content-Type — browser sets it with the correct multipart boundary
      },
      body: formData,
    }).then(async (res) => {
      if (!res.ok) {
        const err = await res.text();
        console.error('ElevenLabs error body:', err);
        throw new Error(`ElevenLabs ${res.status}: ${err}`);
      }
      const data = await res.json();
      return (data.text ?? '') as string;
    });

    return from(promise);
  }
}
