import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

// Keeps already-authenticated users off the marketing/auth pages (home, login,
// register) and sends them straight to their reports instead.
export const guestGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isLoggedIn()) {
    router.navigate(['/reports']);
    return false;
  }

  return true;
};
