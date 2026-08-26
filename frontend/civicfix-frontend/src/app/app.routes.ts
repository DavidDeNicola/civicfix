import { CreateReportComponent } from './features/reports/create-report/create-report.component';
import { Routes } from '@angular/router';
import { LoginComponent } from './features/auth/login/login.component';
import { RegisterComponent } from './features/auth/register/register.component';
import { ReportsListComponent } from './features/reports/reports-list/reports-list.component';
import { authGuard } from './core/guards/auth.guard';
import { ReportDetailComponent } from './features/reports/report-detail/report-detail.component';
import { AdminDashboardComponent } from './features/admin/admin-dashboard/admin-dashboard.component';
import { adminGuard } from './core/guards/admin.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', title: 'Accedi', component: LoginComponent },
  { path: 'register', title: 'Registrati', component: RegisterComponent },
  { path: 'reports', title: 'Segnalazioni', component: ReportsListComponent, canActivate: [authGuard] },
  { path: 'reports/new', title: 'Nuova segnalazione', component: CreateReportComponent, canActivate: [authGuard] },
  { path: 'reports/:id', title: 'Dettaglio segnalazione', component: ReportDetailComponent, canActivate: [authGuard] },
  { path: 'admin', title: 'Amministrazione', component: AdminDashboardComponent, canActivate: [authGuard, adminGuard] }
];
