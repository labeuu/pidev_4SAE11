import { HttpErrorResponse } from '@angular/common/http';

/** Map Angular/REST errors to short UI copy (ticket flows and similar). */
export function messageFromHttpError(err: unknown, fallback: string): string {
  if (err instanceof HttpErrorResponse) {
    if (err.status === 401) {
      return 'Your session may have expired. Please sign in again.';
    }
    if (err.status === 403) {
      const m = extractBackendMessage(err.error);
      return m || 'You do not have permission to perform this action.';
    }
    if (err.status === 404) {
      return extractBackendMessage(err.error) || 'The requested resource was not found.';
    }
    if (err.status === 502 || err.status === 503) {
      const m = extractBackendMessage(err.error);
      return m || 'A backend service is temporarily unavailable. Try again in a moment.';
    }
    const m = extractBackendMessage(err.error);
    if (m) return m;
    if (err.status === 0) {
      return 'Network error. Check your connection and that the API is running.';
    }
  }
  return fallback;
}

function extractBackendMessage(body: unknown): string | null {
  if (typeof body === 'string' && body.trim().length > 0) {
    const s = body.trim();
    return s.length > 240 ? s.slice(0, 240) + '…' : s;
  }
  if (body && typeof body === 'object') {
    const o = body as { message?: unknown; errors?: unknown };
    if (o.errors && typeof o.errors === 'object' && o.errors !== null) {
      const entries = Object.entries(o.errors as Record<string, unknown>)
        .map(([k, v]) => (typeof v === 'string' && v.trim() ? `${k}: ${v}` : null))
        .filter(Boolean) as string[];
      if (entries.length > 0) {
        const joined = entries.join(' ');
        return joined.length > 240 ? joined.slice(0, 240) + '…' : joined;
      }
    }
    if ('message' in o) {
      const m = o.message;
      if (typeof m === 'string' && m.trim()) {
        return m.length > 240 ? m.slice(0, 240) + '…' : m;
      }
    }
  }
  return null;
}
