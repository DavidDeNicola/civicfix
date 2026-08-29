import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

// Tiene gli utenti già autenticati fuori dalle pagine pubbliche (home, login,
// registrazione), rimandandoli direttamente alle segnalazioni.

export const guestGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isLoggedIn()) {
    router.navigate(['/reports']);
    return false;
  }

  return true;
};
