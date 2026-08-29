import {CanActivateFn, Router} from '@angular/router';
import {AuthService} from '../services/auth.service';
import {inject} from '@angular/core';

// Blocca l'accesso alle rotte protette se non c'è un token valido: rimanda al login.

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isLoggedIn()){
    return true;
  } else {
    router.navigateByUrl('/login');
    return false;
  }
};
