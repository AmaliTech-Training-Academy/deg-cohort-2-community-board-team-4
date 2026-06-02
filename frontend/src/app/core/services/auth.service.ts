import { Injectable, signal, computed } from '@angular/core';
import { Observable, of, throwError, timer } from 'rxjs';
import { delay, switchMap } from 'rxjs/operators';
import { User, AuthResponse } from '../models/user.interface';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  // Key names for local storage
  private readonly USERS_KEY = 'ping_users';
  private readonly TOKEN_KEY = 'ping_token';
  private readonly REFRESH_TOKEN_KEY = 'ping_refresh_token';

  // Signals for local reactive state
  private currentUserSignal = signal<User | null>(null);
  
  // Publicly exposed read-only signals
  currentUser = this.currentUserSignal.asReadonly();
  isAuthenticated = computed(() => this.currentUserSignal() !== null);

  constructor() {
    this.initializeMockUsers();
    this.restoreSession();
  }

  /**
   * Seeds default admin & user accounts matching PostgreSQL seeds if storage is empty.
   */
  private initializeMockUsers(): void {
    const storedUsers = localStorage.getItem(this.USERS_KEY);
    if (!storedUsers) {
      const defaultUsers = [
        {
          id: 1,
          email: 'admin@amalitech.com',
          name: 'Admin User',
          password: 'password123', // Raw password for local verification
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

  /**
   * Checks if user has a valid active token and restores state.
   */
  private restoreSession(): void {
    const token = localStorage.getItem(this.TOKEN_KEY);
    if (token) {
      try {
        // Mock token decodes or reads active user from saved list
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

  /**
   * Authenticates user against local storage mock accounts.
   */
  login(email: string, passwordInput: string): Observable<AuthResponse> {
    const users = this.getStoredUsers();
    const matchedUser = users.find(u => u.email.toLowerCase() === email.toLowerCase());

    if (!matchedUser || matchedUser.password !== passwordInput) {
      // Simulate API lag before throwing auth error
      return timer(800).pipe(
        switchMap(() => throwError(() => new Error('Invalid email or password')))
      );
    }

    const { password, ...userWithoutPassword } = matchedUser;
    
    // Generate mock tokens
    const token = this.generateMockToken(matchedUser.email);
    const refreshToken = this.generateMockToken(matchedUser.email, true);

    const response: AuthResponse = {
      token,
      refreshToken,
      user: userWithoutPassword
    };

    return of(response).pipe(
      delay(800), // Simulate network delay
      switchMap(() => {
        localStorage.setItem(this.TOKEN_KEY, token);
        localStorage.setItem(this.REFRESH_TOKEN_KEY, refreshToken);
        this.currentUserSignal.set(userWithoutPassword);
        return of(response);
      })
    );
  }

  /**
   * Registers a new user locally and logs them in.
   */
  register(name: string, email: string, passwordInput: string): Observable<AuthResponse> {
    const users = this.getStoredUsers();
    const isDuplicate = users.some(u => u.email.toLowerCase() === email.toLowerCase());

    if (isDuplicate) {
      // Simulate API lag before throwing duplicate conflict error
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

  /**
   * De-authenticates user and removes session storage.
   */
  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_TOKEN_KEY);
    this.currentUserSignal.set(null);
  }

  /**
   * Accessor for JWT Access Token.
   */
  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  /**
   * Accessor for JWT Refresh Token.
   */
  getRefreshToken(): string | null {
    return localStorage.getItem(this.REFRESH_TOKEN_KEY);
  }

  // --- Mock Token Helpers ---

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
