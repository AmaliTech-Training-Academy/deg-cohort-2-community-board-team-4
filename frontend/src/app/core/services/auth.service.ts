import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap, map } from 'rxjs/operators';
import { User, AuthResponse } from '../models/user.interface';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly API_URL = `${environment.apiUrl}/auth`;
  private readonly TOKEN_KEY = 'ping_token';
  private readonly REFRESH_TOKEN_KEY = 'ping_refresh_token';

  private http = inject(HttpClient);
  private currentUserSignal = signal<User | null>(null);
  
  currentUser = this.currentUserSignal.asReadonly();
  isAuthenticated = computed(() => this.currentUserSignal() !== null);

  constructor() {
    this.restoreSession();
  }

  private restoreSession(): void {
    const token = localStorage.getItem(this.TOKEN_KEY);
    if (token) {
      const user = this.getUserFromToken(token);
      if (user) {
        this.currentUserSignal.set(user);
      } else {
        this.logout();
      }
    }
  }

  login(email: string, passwordInput: string): Observable<AuthResponse> {
    return this.http.post<any>(`${this.API_URL}/login`, {
      email,
      password: passwordInput
    }).pipe(
      tap(res => {
        if (res && res.token) {
          localStorage.setItem(this.TOKEN_KEY, res.token);
          const user = this.getUserFromToken(res.token);
          this.currentUserSignal.set(user);
        }
      }),
      map(res => this.mapBackendResponse(res))
    );
  }

  register(name: string, email: string, passwordInput: string): Observable<AuthResponse> {
    return this.http.post<any>(`${this.API_URL}/register`, {
      name,
      email,
      password: passwordInput
    }).pipe(
      tap(res => {
        if (res && res.token) {
          localStorage.setItem(this.TOKEN_KEY, res.token);
          const user = this.getUserFromToken(res.token);
          this.currentUserSignal.set(user);
        }
      }),
      map(res => this.mapBackendResponse(res))
    );
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_TOKEN_KEY);
    this.currentUserSignal.set(null);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(this.REFRESH_TOKEN_KEY);
  }

  private getUserFromToken(token: string): User | null {
    try {
      const parts = token.split('.');
      if (parts.length !== 3) return null;
      
      // Decodes Base64Url payload to reconstruct user state
      const payloadStr = atob(parts[1].replace(/-/g, '+').replace(/_/g, '/'));
      const payload = JSON.parse(payloadStr);

      if (payload.exp && payload.exp * 1000 < Date.now()) {
        return null;
      }

      return {
        id: Number(payload.sub),
        email: payload.email,
        name: payload.name,
        role: payload.role as 'ADMIN' | 'USER'
      };
    } catch {
      return null;
    }
  }

  private mapBackendResponse(res: any): AuthResponse {
    const user = this.getUserFromToken(res.token);
    return {
      token: res.token,
      refreshToken: '',
      user: user || {
        id: 0,
        email: res.email,
        name: res.name,
        role: res.role as 'ADMIN' | 'USER'
      }
    };
  }
}
