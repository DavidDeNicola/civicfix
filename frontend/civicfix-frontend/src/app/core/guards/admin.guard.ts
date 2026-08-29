import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { Role } from '../models/user.model';

// Consente l'accesso solo a chi ha ruolo ADMIN, altrimenti rimanda alla lista segnalazioni.

export const adminGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.role === Role.ADMIN) {
    return true;
  }

  router.navigate(['/reports']);
  return false;
};
