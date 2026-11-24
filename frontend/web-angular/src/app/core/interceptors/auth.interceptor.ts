import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../auth/auth.service';

/** Routes publiques qui ne nécessitent pas de token (et rejetteraient un token expiré). */
const PUBLIC_PATTERNS = ['/api/directory/', '/api/specialties', '/api/languages'];

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  // Routes d'authentification : jamais de token
  if (req.url.includes('/api/auth/')) {
    return next(req);
  }
  // Routes publiques de l'annuaire : on ne joint pas le token pour éviter un 401
  // si le token est expiré (Spring Security rejette un token invalide même sur permitAll)
  const isPublic = PUBLIC_PATTERNS.some((p) => req.url.includes(p));
  if (isPublic) {
    return next(req);
  }
  const token = inject(AuthService).token();
  if (token) {
    return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
  }
  return next(req);
};