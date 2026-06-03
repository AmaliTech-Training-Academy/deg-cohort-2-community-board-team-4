import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { User, AuthResponse } from '../models/user.interface';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  const TOKEN_KEY = 'ping_token';
  const REFRESH_TOKEN_KEY = 'ping_refresh_token';

  // Helper to generate a valid padded Base64Url JWT for tests
  function createMockToken(sub: string, email: string, name: string, role: string, expOffset: number): string {
    const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
    const exp = Math.floor(Date.now() / 1000) + expOffset;
    const payload = btoa(JSON.stringify({ sub, email, name, role, exp }));
    const safePayload = payload.replace(/\+/g, '-').replace(/\//g, '_');
    return `${header}.${safePayload}.mocksignature`;
  }

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('Initialization / Session Restore', () => {
    it('should restore active session if valid token exists in storage', () => {
      const mockToken = createMockToken('12', 'user@example.com', 'Test User', 'USER', 3600);
      localStorage.setItem(TOKEN_KEY, mockToken);

      // Instantiates service to trigger session restoration in constructor
      service = TestBed.inject(AuthService);

      expect(service.isAuthenticated()).toBeTrue();
      expect(service.currentUser()).toEqual({
        id: 12,
        email: 'user@example.com',
        name: 'Test User',
        role: 'USER'
      });
    });

    it('should discard session and call logout if token is expired', () => {
      const expiredToken = createMockToken('12', 'user@example.com', 'Test User', 'USER', -3600);
      localStorage.setItem(TOKEN_KEY, expiredToken);

      service = TestBed.inject(AuthService);

      expect(service.isAuthenticated()).toBeFalse();
      expect(service.currentUser()).toBeNull();
      expect(localStorage.getItem(TOKEN_KEY)).toBeNull();
    });
  });

  describe('API Requests & Data Mapping', () => {
    beforeEach(() => {
      service = TestBed.inject(AuthService);
    });

    it('should authenticate user and store token on login success (200)', () => {
      const mockToken = createMockToken('42', 'admin@example.com', 'Admin User', 'ADMIN', 3600);
      const backendResponse = {
        token: mockToken,
        email: 'admin@example.com',
        name: 'Admin User',
        role: 'ADMIN'
      };

      service.login('admin@example.com', 'password123').subscribe((res: AuthResponse) => {
        expect(res.token).toBe(mockToken);
        expect(res.user.id).toBe(42);
        expect(res.user.email).toBe('admin@example.com');
        expect(res.user.name).toBe('Admin User');
        expect(res.user.role).toBe('ADMIN');
        
        expect(service.isAuthenticated()).toBeTrue();
        expect(service.currentUser()?.id).toBe(42);
        expect(localStorage.getItem(TOKEN_KEY)).toBe(mockToken);
      });

      const req = httpMock.expectOne('http://localhost:8080/api/auth/login');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ email: 'admin@example.com', password: 'password123' });
      req.flush(backendResponse);
    });

    it('should propagate unauthorized error on login failure (401)', () => {
      service.login('wrong@example.com', 'badpass').subscribe({
        next: () => fail('Expected login to fail with 401'),
        error: (error) => {
          expect(error.status).toBe(401);
          expect(service.isAuthenticated()).toBeFalse();
          expect(service.currentUser()).toBeNull();
        }
      });

      const req = httpMock.expectOne('http://localhost:8080/api/auth/login');
      req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
    });

    it('should register user and update active session on registration success (201)', () => {
      const mockToken = createMockToken('8', 'newuser@example.com', 'New User', 'USER', 3600);
      const backendResponse = {
        token: mockToken,
        email: 'newuser@example.com',
        name: 'New User',
        role: 'USER'
      };

      service.register('New User', 'newuser@example.com', 'securepass123').subscribe((res: AuthResponse) => {
        expect(res.token).toBe(mockToken);
        expect(res.user.id).toBe(8);
        expect(service.isAuthenticated()).toBeTrue();
        expect(service.currentUser()?.name).toBe('New User');
        expect(localStorage.getItem(TOKEN_KEY)).toBe(mockToken);
      });

      const req = httpMock.expectOne('http://localhost:8080/api/auth/register');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({
        name: 'New User',
        email: 'newuser@example.com',
        password: 'securepass123'
      });
      req.flush(backendResponse, { status: 201, statusText: 'Created' });
    });

    it('should propagate duplicate error on registration conflict (409)', () => {
      service.register('New User', 'duplicate@example.com', 'pass123').subscribe({
        next: () => fail('Expected register to fail with 409'),
        error: (error) => {
          expect(error.status).toBe(409);
          expect(service.isAuthenticated()).toBeFalse();
          expect(service.currentUser()).toBeNull();
        }
      });

      const req = httpMock.expectOne('http://localhost:8080/api/auth/register');
      req.flush('Conflict', { status: 409, statusText: 'Conflict' });
    });
  });

  describe('Session Expiry & Lifecycle', () => {
    beforeEach(() => {
      service = TestBed.inject(AuthService);
    });

    it('should purge tokens and reset user signal on logout', () => {
      const mockToken = createMockToken('1', 'logout@example.com', 'Logout User', 'USER', 3600);
      localStorage.setItem(TOKEN_KEY, mockToken);
      localStorage.setItem(REFRESH_TOKEN_KEY, 'refresh-token-val');

      service.logout();

      expect(service.isAuthenticated()).toBeFalse();
      expect(service.currentUser()).toBeNull();
      expect(localStorage.getItem(TOKEN_KEY)).toBeNull();
      expect(localStorage.getItem(REFRESH_TOKEN_KEY)).toBeNull();
    });
  });
});
