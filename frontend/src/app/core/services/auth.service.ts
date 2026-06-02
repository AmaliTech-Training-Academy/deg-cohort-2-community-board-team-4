import { Injectable, signal, computed } from '@angular/core';
import { Observable, of, throwError, timer } from 'rxjs';
import { delay, switchMap } from 'rxjs/operators';
import { User, AuthResponse } from '../models/user.interface';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly USERS_KEY = 'ping_users';
  private readonly TOKEN_KEY = 'ping_token';
  private readonly REFRESH_TOKEN_KEY = 'ping_refresh_token';

  private currentUserSignal = signal<User | null>(null);
  
  currentUser = this.currentUserSignal.asReadonly();
  isAuthenticated = computed(() => this.currentUserSignal() !== null);

  constructor() {
    this.initializeMockUsers();
    this.restoreSession();
  }

  private initializeMockUsers(): void {
    const storedUsers = localStorage.getItem(this.USERS_KEY);
    if (!storedUsers) {
      const defaultUsers = [
        {
          id: 1,
          email: 'admin@amalitech.com',
          name: 'Admin User',
          password: 'password123',
          role: 'ADMIN' as const,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString()
        },
        {
          id: 2,
          email: 'user@amalitech.com',
          name: 'Test User',
          password: 'password123',
          role: 'USER' as const,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString()
        }
      ];
      localStorage.setItem(this.USERS_KEY, JSON.stringify(defaultUsers));
    }
  }

  private restoreSession(): void {
    const token = localStorage.getItem(this.TOKEN_KEY);
    if (token) {
      try {
        const email = this.getEmailFromToken(token);
        const users = this.getStoredUsers();
        const matchedUser = users.find(u => u.email === email);
        if (matchedUser) {
          const { password, ...userWithoutPassword } = matchedUser;
          this.currentUserSignal.set(userWithoutPassword);
        } else {
          this.logout();
        }
      } catch {
        this.logout();
      }
    }
  }

  login(email: string, passwordInput: string): Observable<AuthResponse> {
    const users = this.getStoredUsers();
    const matchedUser = users.find(u => u.email.toLowerCase() === email.toLowerCase());

    if (!matchedUser || matchedUser.password !== passwordInput) {
      return timer(800).pipe(
        switchMap(() => throwError(() => new Error('Invalid email or password')))
      );
    }

    const { password, ...userWithoutPassword } = matchedUser;
    
    const token = this.generateMockToken(matchedUser.email);
    const refreshToken = this.generateMockToken(matchedUser.email, true);

    const response: AuthResponse = {
      token,
      refreshToken,
      user: userWithoutPassword
    };

    return of(response).pipe(
      delay(800),
      switchMap(() => {
        localStorage.setItem(this.TOKEN_KEY, token);
        localStorage.setItem(this.REFRESH_TOKEN_KEY, refreshToken);
        this.currentUserSignal.set(userWithoutPassword);
        return of(response);
      })
    );
  }

  register(name: string, email: string, passwordInput: string): Observable<AuthResponse> {
    const users = this.getStoredUsers();
    const isDuplicate = users.some(u => u.email.toLowerCase() === email.toLowerCase());

    if (isDuplicate) {
      return timer(800).pipe(
        switchMap(() => throwError(() => new Error('Email is already registered')))
      );
    }

    const newUser = {
      id: users.length + 1,
      email,
      name,
      password: passwordInput,
      role: 'USER' as const,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    };

    users.push(newUser);
    localStorage.setItem(this.USERS_KEY, JSON.stringify(users));

    const { password, ...userWithoutPassword } = newUser;
    const token = this.generateMockToken(newUser.email);
    const refreshToken = this.generateMockToken(newUser.email, true);

    const response: AuthResponse = {
      token,
      refreshToken,
      user: userWithoutPassword
    };

    return of(response).pipe(
      delay(800),
      switchMap(() => {
        localStorage.setItem(this.TOKEN_KEY, token);
        localStorage.setItem(this.REFRESH_TOKEN_KEY, refreshToken);
        this.currentUserSignal.set(userWithoutPassword);
        return of(response);
      })
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

  private getStoredUsers(): any[] {
    const data = localStorage.getItem(this.USERS_KEY);
    return data ? JSON.parse(data) : [];
  }

  private generateMockToken(email: string, isRefresh = false): string {
    const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
    const payload = btoa(JSON.stringify({
      sub: email,
      role: email === 'admin@amalitech.com' ? 'ADMIN' : 'USER',
      type: isRefresh ? 'refresh' : 'access',
      exp: Math.floor(Date.now() / 1000) + (isRefresh ? 86400 : 3600) // 1 day or 1 hour
    }));
    const signature = btoa('mock-hmac-signature-256');
    return `${header}.${payload}.${signature}`;
  }

  private getEmailFromToken(token: string): string {
    try {
      const parts = token.split('.');
      if (parts.length !== 3) return '';
      const payload = JSON.parse(atob(parts[1]));
      return payload.sub || '';
    } catch {
      return '';
    }
  }
}
