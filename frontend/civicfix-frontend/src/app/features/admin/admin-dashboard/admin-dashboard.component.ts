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
import { Observable, concat, last } from 'rxjs';
import { AdminService } from '../../../core/services/admin.service';
import { ReportService } from '../../../core/services/report.service';
import { AdminUser, CreateUserRequest, CreateTeamRequest, Team } from '../../../core/models/admin.model';
import { Role } from '../../../core/models/user.model';
import { Report, ReportCategory, ReportPriority, ReportStatus } from '../../../core/models/report.model';
import { UserDialogComponent } from '../user-dialog/user-dialog.component';
import { TeamDialogComponent } from '../team-dialog/team-dialog.component';
import {
  CategoriaPipe, PrioritaPipe, RuoloPipe, StatoPipe, ETICHETTE_RUOLO_PLURALE
} from '../../../core/pipes/etichette.pipe';
import { ICONE_CATEGORIA } from '../../../core/constants/category-icons';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatTabsModule, MatFormFieldModule,
    MatInputModule, MatSelectModule, MatButtonModule, MatProgressSpinnerModule,
    MatIconModule, MatDialogModule, StatoPipe, PrioritaPipe, CategoriaPipe, RuoloPipe
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

  /** Ordine di presentazione dei gruppi nella scheda Utenti. */
  readonly ruoliOrdinati: Role[] = [Role.ADMIN, Role.OPERATOR, Role.CITIZEN];

  readonly iconeRuolo: Record<string, string> = {
    CITIZEN: 'person',
    OPERATOR: 'engineering',
    ADMIN: 'shield_person'
  };

  prioritaDisponibili = Object.values(ReportPriority);

  teamSceltoPerReport: { [reportId: number]: number } = {};
  operatoreSceltoPerReport: { [reportId: number]: number } = {};
  prioritaSceltaPerReport: { [reportId: number]: ReportPriority } = {};
  salvataggioPerReport: { [reportId: number]: boolean } = {};

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
      next: (users) => { this.users = users; this.loading = false; this.allineaSelezioni(); },
      error: () => { this.errore = 'Impossibile caricare gli utenti.'; this.loading = false; }
    });

    this.adminService.getTeams().subscribe({
      next: (teams) => { this.teams = teams; this.allineaSelezioni(); }
    });

    this.reportService.findAll(0, 100).subscribe({
      next: (response) => { this.reports = response.content; this.allineaSelezioni(); }
    });
  }

  /**
   * Precarica i menu a tendina con l'assegnazione già in vigore, così le
   * schede mostrano lo stato attuale e il salvataggio può inviare soltanto
   * ciò che l'amministratore ha davvero cambiato.
   */
  private allineaSelezioni(): void {
    for (const report of this.reports) {
      const teamId = this.teamIdCorrente(report);
      if (teamId) this.teamSceltoPerReport[report.id] = teamId;

      const operatoreId = this.operatoreIdCorrente(report);
      if (operatoreId) this.operatoreSceltoPerReport[report.id] = operatoreId;

      this.prioritaSceltaPerReport[report.id] = report.priority;
    }
  }

  // Le segnalazioni espongono team e operatore per nome, non per id: qui si
  // risale all'id incrociando le liste già caricate.
  teamIdCorrente(report: Report): number | undefined {
    return this.teams.find(t => t.name === report.assignedTeamName)?.id;
  }

  operatoreIdCorrente(report: Report): number | undefined {
    return this.users.find(u => u.username === report.assignedOperatorUsername)?.id;
  }

  private mostraErrore(msg: string): void {
    this.errore = msg;
    setTimeout(() => this.errore = null, 3500);
  }

  private mostraMessaggio(msg: string): void {
    this.messaggio = msg;
    setTimeout(() => this.messaggio = null, 2500);
  }

  // --- Raggruppamenti ---------------------------------------------------

  utentiPerRuolo(ruolo: Role): AdminUser[] {
    return this.users.filter(u => u.role === ruolo);
  }

  etichettaGruppoRuolo(ruolo: Role): string {
    return ETICHETTE_RUOLO_PLURALE[ruolo] ?? ruolo;
  }

  /** Solo le categorie che hanno almeno un team, nell'ordine dell'enum. */
  get categorieConTeam(): ReportCategory[] {
    return Object.values(ReportCategory)
      .filter(categoria => this.teams.some(t => t.category === categoria));
  }

  teamPerCategoria(categoria: ReportCategory): Team[] {
    return this.teams.filter(t => t.category === categoria);
  }

  iconaCategoria(categoria: string): string {
    return ICONE_CATEGORIA[categoria] ?? 'assignment';
  }

  get operatori(): AdminUser[] {
    return this.users.filter(u => u.role === Role.OPERATOR);
  }

  operatoriDelTeam(teamId: number | undefined): AdminUser[] {
    if (!teamId) return this.operatori;
    return this.operatori.filter(o => o.teamId === teamId);
  }

  puoEssereAssegnata(report: Report): boolean {
    return report.status !== 'RESOLVED' && report.status !== 'REJECTED';
  }

  /**
   * Le segnalazioni aperte sono divise per stato: una già presa in carico non
   * deve restare sotto "Da assegnare", pur rimanendo modificabile.
   */
  get gruppiAperti(): { titolo: string; icona: string; reports: Report[] }[] {
    return [
      {
        titolo: 'Da assegnare',
        icona: 'inbox',
        reports: this.reports.filter(r => r.status === ReportStatus.PENDING)
      },
      {
        titolo: 'In lavorazione',
        icona: 'engineering',
        reports: this.reports.filter(r => r.status === ReportStatus.IN_PROGRESS)
      }
    ];
  }

  get segnalazioniChiuse(): Report[] {
    return this.reports.filter(r => !this.puoEssereAssegnata(r));
  }

  // --- Creazione utenti e team ------------------------------------------

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

  // --- Assegnazione segnalazioni ----------------------------------------

  /**
   * true quando una delle tre tendine differisce dall'assegnazione in vigore.
   * Confronta soltanto valori: il template la interroga a ogni ciclo di
   * change detection, quindi non deve costruire nulla.
   */
  haModifiche(report: Report): boolean {
    const team = this.teamSceltoPerReport[report.id];
    const operatore = this.operatoreSceltoPerReport[report.id];
    const priorita = this.prioritaSceltaPerReport[report.id];

    return (!!team && team !== this.teamIdCorrente(report))
      || (!!operatore && operatore !== this.operatoreIdCorrente(report))
      || (!!priorita && priorita !== report.priority);
  }

  private operazioniDiSalvataggio(report: Report): Observable<Report>[] {
    const operazioni: Observable<Report>[] = [];

    const team = this.teamSceltoPerReport[report.id];
    const operatore = this.operatoreSceltoPerReport[report.id];
    const priorita = this.prioritaSceltaPerReport[report.id];

    // L'ordine è vincolante: il backend rifiuta un operatore che non
    // appartiene al team della segnalazione, quindi il team va prima.
    if (team && team !== this.teamIdCorrente(report)) {
      operazioni.push(this.reportService.assignTeam(report.id, team));
    }
    if (operatore && operatore !== this.operatoreIdCorrente(report)) {
      operazioni.push(this.reportService.assignOperator(report.id, operatore));
    }
    if (priorita && priorita !== report.priority) {
      operazioni.push(this.reportService.assignPriority(report.id, priorita));
    }

    return operazioni;
  }

  salvaAssegnazioni(report: Report): void {
    const operazioni = this.operazioniDiSalvataggio(report);

    if (operazioni.length === 0) {
      this.mostraMessaggio('Nessuna modifica da salvare.');
      return;
    }

    this.salvataggioPerReport[report.id] = true;

    // concat esegue in sequenza e si ferma al primo errore; last() tiene solo
    // la segnalazione risultante dall'ultima operazione andata a buon fine.
    concat(...operazioni).pipe(last()).subscribe({
      next: (aggiornata) => {
        this.salvataggioPerReport[report.id] = false;
        this.aggiornaReportInLista(aggiornata);
      },
      error: (err) => {
        this.salvataggioPerReport[report.id] = false;
        this.mostraErrore(err.status === 409
          ? 'L\'operatore scelto non appartiene al team assegnato.'
          : 'Impossibile salvare le assegnazioni.');
        // Alcune operazioni possono essere già passate: si ricarica per non
        // lasciare le tendine disallineate rispetto ai dati reali.
        this.caricaTutto();
      }
    });
  }

  private aggiornaReportInLista(aggiornata: Report): void {
    this.reports = this.reports.map(r => r.id === aggiornata.id ? aggiornata : r);
    this.allineaSelezioni();
    this.mostraMessaggio('Segnalazione aggiornata.');
  }
}
