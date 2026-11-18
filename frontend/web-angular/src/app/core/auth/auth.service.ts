import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { map, tap } from 'rxjs';
import { API_BASE_URL } from '../api.config';
import { AuthResponse, LoginRequest, RegisterRequest, UserResponse } from './auth.models';

const STORAGE_KEY = 'clinic-copilot.session';
const TOKEN_URL = '/realms/clinic/protocol/openid-connect/token';
const ROLES = ['ADMIN', 'DOCTOR', 'PATIENT'];

/** Décode la charge utile (payload) d'un JWT, sans vérifier la signature (le serveur s'en charge). */
function decodeJwt(token: string): { email?: string; realm_access?: { roles?: string[] } } {
  const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
  const json = decodeURIComponent(
    atob(base64)
      .split('')
      .map((c) => '%' + c.charCodeAt(0).toString(16).padStart(2, '0'))
      .join(''),
  );
  return JSON.parse(json);
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly _session = signal<AuthResponse | null>(this.read());
  readonly session = this._session.asReadonly();
  readonly isLoggedIn = computed(() => this._session() !== null);
  readonly token = computed(() => this._session()?.token ?? null);

  /** Connexion : Keycloak délivre le token (grant "password" du client public clinic-web). */
  login(body: LoginRequest) {
    const form = new HttpParams()
      .set('grant_type', 'password')
      .set('client_id', 'clinic-web')
      .set('username', body.email.trim().toLowerCase())
      .set('password', body.password);

    return this.http
      .post<{ access_token: string }>(TOKEN_URL, form, {
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      })
      .pipe(
        map((res): AuthResponse => {
          const claims = decodeJwt(res.access_token);
          const role = (claims.realm_access?.roles ?? []).find((r) => ROLES.includes(r)) ?? '';
          return { token: res.access_token, email: claims.email ?? body.email, role };
        }),
        tap((r) => this.save(r)),
      );
  }

  register(body: RegisterRequest) {
    return this.http.post<UserResponse>(`${API_BASE_URL}/api/auth/register`, body);
  }

  logout(redirect = true) {
    this._session.set(null);
    try {
      localStorage.removeItem(STORAGE_KEY);
    } catch {
      /* stockage indisponible : on ignore */
    }
    if (redirect) this.router.navigateByUrl('/');
  }

  private save(r: AuthResponse) {
    this._session.set(r);
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(r));
    } catch {
      /* stockage indisponible : la session reste en mémoire */
    }
  }

  private read(): AuthResponse | null {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      return raw ? (JSON.parse(raw) as AuthResponse) : null;
    } catch {
      return null;
    }
  }
}

/** Message lisible à partir de l'erreur HTTP. */
export function authErrorMessage(e: unknown, context: 'login' | 'register'): string {
  if (!(e instanceof HttpErrorResponse)) return 'Une erreur est survenue. Réessayez.';
  switch (e.status) {
    case 0:
      return 'Impossible de joindre le serveur. Vérifiez que le service est démarré.';
    case 400:
      return context === 'login'
        ? 'Email ou mot de passe incorrect, ou compte désactivé.'
        : 'Vérifiez les champs du formulaire.';
    case 401:
    case 404:
      return context === 'login' ? 'Email ou mot de passe incorrect.' : 'Une erreur est survenue. Réessayez.';
    case 403:
      return 'Ce compte est désactivé.';
    case 409:
      return 'Un compte existe déjà avec cet email.';
    default:
      return 'Une erreur est survenue. Réessayez.';
  }
}