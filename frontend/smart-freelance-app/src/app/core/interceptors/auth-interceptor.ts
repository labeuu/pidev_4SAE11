import { HttpInterceptorFn } from '@angular/common/http';

const TOKEN_KEY = 'access_token';

/**
 * Auth Interceptor - Automatically adds JWT token to HTTP requests
 *
 * Reads the token from localStorage or sessionStorage (AuthService stores in
 * either based on "Stay signed in") to avoid requests failing with 401.
 * Uses direct storage read to avoid circular DI (AuthService → HttpClient → interceptor).
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem(TOKEN_KEY) || sessionStorage.getItem(TOKEN_KEY);

  if (token) {
    return next(req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    }));
  }

  return next(req);
};
