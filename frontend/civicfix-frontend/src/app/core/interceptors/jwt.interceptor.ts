import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

/** Solo le richieste dirette al nostro backend devono portare il token. */
const NOSTRA_API = 'http://localhost:8080/api';

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const token = authService.getToken();

  // Senza questo controllo il token finirebbe anche nelle richieste verso
  // servizi di terze parti (es. il geocoding via Nominatim), che non hanno
  // alcun motivo di riceverlo: sarebbe una fuga di credenziali.
  if (token && req.url.startsWith(NOSTRA_API)) {
    req = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });
  }

  return next(req).pipe(
    catchError((error: unknown) => {
      // 401 significa sempre "non sei autenticato" (token assente, scaduto,
      // o riferito a un utente non più esistente, tipico dopo un reset del
      // database in sviluppo): in quel caso non ha senso restare sulla
      // pagina con dati che il server continuerà a rifiutare.
      // Un 403, invece, vuol dire "sei autenticato ma non puoi fare questo"
      // (ruolo insufficiente, azione su una risorsa altrui): forzare il
      // logout in quel caso caccerebbe via un utente valido per un'azione
      // singola non permessa, quindi va lasciato gestire al chiamante.
      const giaSulLogin = router.url.startsWith('/login');
      const eLaNostraApi = req.url.startsWith(NOSTRA_API);
      if (error instanceof HttpErrorResponse && error.status === 401 && eLaNostraApi && !giaSulLogin) {
        authService.logout();
        router.navigate(['/login']);
      }
      return throwError(() => error);
    })
  );
};
