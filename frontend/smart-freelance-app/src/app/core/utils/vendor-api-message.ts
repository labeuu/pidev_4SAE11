/**
 * Corps d'erreur renvoyé par {@link org.example.vendor.exception.GlobalExceptionHandler.ErrorResponse}.
 */
export interface VendorApiErrorBody {
  status?: number;
  message?: string;
  timestamp?: string;
}

/**
 * Extrait le message utilisateur à partir d'une erreur HttpClient / backend Vendor.
 */
export function parseVendorApiMessage(err: unknown, fallback: string): string {
  const e = err as {
    error?: VendorApiErrorBody | Record<string, unknown>;
    message?: string;
  };
  const body = e?.error;
  if (body && typeof body === 'object' && 'message' in body) {
    const m = (body as VendorApiErrorBody).message;
    if (typeof m === 'string' && m.length > 0) {
      return m;
    }
  }
  if (typeof e?.message === 'string' && e.message.length > 0) {
    return e.message;
  }
  return fallback;
}
