import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Guard to prevent unauthenticated access to protected routes (e.g., /dashboard).
 * Redirects to /auth/login.
 */
export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    return true;
  }

  // Redirect to login if not authenticated
  return router.createUrlTree(['/auth/login']);
};

/**
 * Guard to prevent authenticated users from visiting auth pages (e.g., login, register).
 * Redirects to /dashboard.
 */
export const publicGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    // Redirect to dashboard if already logged in
    return router.createUrlTree(['/dashboard']);
  }

  return true;
};
