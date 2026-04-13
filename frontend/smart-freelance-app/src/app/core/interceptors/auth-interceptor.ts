import { HttpInterceptorFn } from '@angular/common/http';

const TOKEN_KEY   = 'access_token';
const USER_ID_KEY = 'user_id';

/**
 * Auth Interceptor - Automatically adds JWT token to HTTP requests
 *
 * Reads the token from localStorage or sessionStorage (AuthService stores in
 * either based on "Stay signed in") to avoid requests failing with 401.
 * Also injects X-User-Id so microservices that rely on header-based identity
 * (e.g. Meeting service) receive the caller's database user ID.
 * Uses direct storage read to avoid circular DI (AuthService → HttpClient → interceptor).
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token  = localStorage.getItem(TOKEN_KEY)   || sessionStorage.getItem(TOKEN_KEY);
  const userId = localStorage.getItem(USER_ID_KEY) || sessionStorage.getItem(USER_ID_KEY);

  if (token) {
    const headers: Record<string, string> = { Authorization: `Bearer ${token}` };
    if (userId) headers['X-User-Id'] = userId;
    return next(req.clone({ setHeaders: headers }));
  }

  return next(req);
};
