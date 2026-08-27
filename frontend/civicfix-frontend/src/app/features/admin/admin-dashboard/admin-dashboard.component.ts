import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTabsModule } from '@angular/material/tabs';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { AdminService } from '../../../core/services/admin.service';
import { ReportService } from '../../../core/services/report.service';
import { AdminUser, CreateUserRequest, CreateTeamRequest, Team } from '../../../core/models/admin.model';
import { Role } from '../../../core/models/user.model';
import { Report } from '../../../core/models/report.model';
import { ReportPriority } from '../../../core/models/report.model';
import { UserDialogComponent } from '../user-dialog/user-dialog.component';
import { TeamDialogComponent } from '../team-dialog/team-dialog.component';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatTabsModule, MatFormFieldModule,
    MatInputModule, MatSelectModule, MatButtonModule, MatProgressSpinnerModule,
    MatIconModule, MatDialogModule
  ],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.scss'
})
export class AdminDashboardComponent implements OnInit {
  users: AdminUser[] = [];
  teams: Team[] = [];
  reports: Report[] = [];

  loading: boolean = true;
  errore: string | null = null;
  messaggio: string | null = null;

  teamSceltoPerReport: { [reportId: number]: number } = {};
  prioritaDisponibili = Object.values(ReportPriority);
  prioritaSceltaPerReport: { [reportId: number]: ReportPriority } = {};
  operatoreSceltoPerReport: { [reportId: number]: number } = {};

  constructor(
    private adminService: AdminService,
    private reportService: ReportService,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.caricaTutto();
  }

  caricaTutto(): void {
    this.loading = true;

    this.adminService.getUsers().subscribe({
      next: (users) => { this.users = users; this.loading = false; },
      error: () => { this.errore = 'Impossibile caricare gli utenti.'; this.loading = false; }
    });

    this.adminService.getTeams().subscribe({
      next: (teams) => this.teams = teams
    });

    this.reportService.findAll(0, 100).subscribe({
      next: (response) => this.reports = response.content
    });
  }

  private mostraErrore(msg: string): void {
    this.errore = msg;
    setTimeout(() => this.errore = null, 2500);
  }

  private mostraMessaggio(msg: string): void {
    this.messaggio = msg;
    setTimeout(() => this.messaggio = null, 2500);
  }

  get operatori(): AdminUser[] {
    return this.users.filter(u => u.role === Role.OPERATOR);
  }

  puoEssereAssegnata(report: Report): boolean {
    return report.status !== 'RESOLVED' && report.status !== 'REJECTED';
  }

  apriDialogUtente(): void {
    const ref = this.dialog.open(UserDialogComponent, {
      data: { teams: this.teams },
      autoFocus: 'first-tabbable',
      restoreFocus: true
    });

    ref.afterClosed().subscribe((richiesta: CreateUserRequest | undefined) => {
      if (richiesta) {
        this.creaUtente(richiesta);
      }
    });
  }

  private creaUtente(richiesta: CreateUserRequest): void {
    this.errore = null;
    this.messaggio = null;

    this.adminService.createUser(richiesta).subscribe({
      next: (utente) => {
        this.users = [...this.users, utente];
        this.mostraMessaggio(`Utente ${utente.username} creato.`);
      },
      error: (err) => {
        this.mostraErrore(err.status === 409 ? 'Username o email già in uso.' : 'Impossibile creare l\'utente.');
      }
    });
  }

  apriDialogTeam(): void {
    const ref = this.dialog.open(TeamDialogComponent, {
      autoFocus: 'first-tabbable',
      restoreFocus: true
    });

    ref.afterClosed().subscribe((richiesta: CreateTeamRequest | undefined) => {
      if (richiesta) {
        this.creaTeam(richiesta);
      }
    });
  }

  private creaTeam(richiesta: CreateTeamRequest): void {
    this.errore = null;
    this.messaggio = null;

    this.adminService.createTeam(richiesta).subscribe({
      next: (team) => {
        this.teams = [...this.teams, team];
        this.mostraMessaggio(`Team ${team.name} creato.`);
      },
      error: () => this.mostraErrore('Impossibile creare il team.')
    });
  }

  assegnaTeamUtente(userId: number, teamId: number): void {
    this.adminService.assignUserTeam(userId, teamId).subscribe({
      next: (utenteAggiornato) => {
        this.users = this.users.map(u => u.id === userId ? utenteAggiornato : u);
        this.mostraMessaggio('Team assegnato all\'operatore.');
      },
      error: () => this.mostraErrore('Impossibile assegnare il team.')
    });
  }

  assegnaTeamReport(report: Report): void {
    const teamId = this.teamSceltoPerReport[report.id];
    if (!teamId) return;

    this.reportService.assignTeam(report.id, teamId).subscribe({
      next: (aggiornata) => this.aggiornaReportInLista(aggiornata),
      error: () => this.mostraErrore('Impossibile assegnare il team alla segnalazione.')

    });
  }

  assegnaOperatoreReport(report: Report): void {
    const operatorId = this.operatoreSceltoPerReport[report.id];
    if (!operatorId) return;

    this.reportService.assignOperator(report.id, operatorId).subscribe({
      next: (aggiornata) => this.aggiornaReportInLista(aggiornata),
      error: (err) => {
        this.mostraErrore(err.status === 409
          ? 'L\'operatore scelto non appartiene al team assegnato.'
          : 'Impossibile assegnare l\'operatore.');
      }
    });
  }

  assegnaPriorita(report: Report): void {
    const priorita = this.prioritaSceltaPerReport[report.id];
    if (!priorita) return;

    this.reportService.assignPriority(report.id, priorita).subscribe({
      next: (aggiornata) => this.aggiornaReportInLista(aggiornata),
      error: () => this.mostraErrore('Impossibile assegnare la priorità.')
    });
  }

  private aggiornaReportInLista(aggiornata: Report): void {
    this.reports = this.reports.map(r => r.id === aggiornata.id ? aggiornata : r);
    this.mostraMessaggio('Segnalazione aggiornata.');
  }
}
