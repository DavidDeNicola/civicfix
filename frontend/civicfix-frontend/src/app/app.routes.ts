import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { guestGuard } from './core/guards/guest.guard';
import { adminGuard } from './core/guards/admin.guard';

// Ogni rotta è caricata su richiesta: le pagine con Leaflet e la dashboard di
// amministrazione sono le più pesanti dell'app e, importate staticamente,
// finivano tutte nel bundle iniziale.
export const routes: Routes = [
  // Nessun guard: la home resta raggiungibile dal logo anche da autenticati.
  {
    path: '',
    title: 'CivicFix',
    loadComponent: () => import('./features/home/home.component').then(m => m.HomeComponent)
  },
  {
    path: 'login',
    title: 'Accedi',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'register',
    title: 'Registrati',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent)
  },
  {
    path: 'forgot-password',
    title: 'Password dimenticata',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/forgot-password/forgot-password.component').then(m => m.ForgotPasswordComponent)
  },
  // Volutamente senza guard: il link arrivato per email deve funzionare anche
  // se in quel browser c'è già una sessione aperta.
  {
    path: 'reset-password',
    title: 'Nuova password',
    loadComponent: () => import('./features/auth/reset-password/reset-password.component').then(m => m.ResetPasswordComponent)
  },
  {
    path: 'reports',
    title: 'Segnalazioni',
    canActivate: [authGuard],
    loadComponent: () => import('./features/reports/reports-list/reports-list.component').then(m => m.ReportsListComponent)
  },
  {
    path: 'reports/new',
    title: 'Nuova segnalazione',
    canActivate: [authGuard],
    loadComponent: () => import('./features/reports/create-report/create-report.component').then(m => m.CreateReportComponent)
  },
  {
    path: 'reports/:id',
    title: 'Dettaglio segnalazione',
    canActivate: [authGuard],
    loadComponent: () => import('./features/reports/report-detail/report-detail.component').then(m => m.ReportDetailComponent)
  },
  {
    path: 'admin',
    title: 'Amministrazione',
    canActivate: [authGuard, adminGuard],
    loadComponent: () => import('./features/admin/admin-dashboard/admin-dashboard.component').then(m => m.AdminDashboardComponent)
  }
];
