import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { jwtInterceptor } from './core/interceptors/jwt.interceptor';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';

// Nota: le etichette italiane del paginatore sono fornite dal componente che
// lo usa, non qui. Importare MatPaginatorIntl a livello di applicazione
// trascinerebbe il modulo paginatore nel bundle iniziale (+216 kB), vanificando
// il caricamento su richiesta della pagina segnalazioni.
export const appConfig: ApplicationConfig = {
  providers: [
    provideAnimationsAsync(),
    provideHttpClient(withInterceptors([jwtInterceptor])),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes)]
};
