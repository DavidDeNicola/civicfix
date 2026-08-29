import { Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
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
import { Observable, Subscription, concat, last } from 'rxjs';
import { Chart, ChartOptions, registerables } from 'chart.js';
import jsPDF from 'jspdf';
import { AdminService } from '../../../core/services/admin.service';
import { ReportService } from '../../../core/services/report.service';
import { ThemeService } from '../../../core/services/theme.service';
import { AdminUser, CreateUserRequest, CreateTeamRequest, Team, Statistics } from '../../../core/models/admin.model';
import { Role } from '../../../core/models/user.model';
import { Report, ReportCategory, ReportPriority, ReportStatus } from '../../../core/models/report.model';
import { UserDialogComponent } from '../user-dialog/user-dialog.component';
import { TeamDialogComponent } from '../team-dialog/team-dialog.component';
import {
  CategoriaPipe, PrioritaPipe, RuoloPipe, StatoPipe, ETICHETTE_RUOLO_PLURALE,
  ETICHETTE_STATO, ETICHETTE_CATEGORIA, ETICHETTE_PRIORITA
} from '../../../core/pipes/etichette.pipe';
import { ICONE_CATEGORIA } from '../../../core/constants/category-icons';

Chart.register(...registerables);

// Colori fissi usati solo per l'esportazione PDF: il tema scuro disegna le
// etichette in un colore chiaro, illeggibile sulla pagina bianca del PDF.
const TESTO_STAMPA = '#1e293b';
const GRIGLIA_STAMPA = '#e2e8f0';

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

/**
 * Pannello di amministrazione: tre viste in una — gestione utenti/team,
 * assegnazione delle segnalazioni aperte, statistiche con grafici
 * (Chart.js) esportabili in CSV e PDF. È un componente grande perché
 * raccoglie funzionalità diverse sotto le stesse tab, non perché una
 * singola funzionalità sia complessa.
 */
export class AdminDashboardComponent implements OnInit, OnDestroy {
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

  statistiche: Statistics | null = null;
  caricamentoStatistiche: boolean = true;
  erroreStatistiche: string | null = null;

  readonly statiPerGrafico: ReportStatus[] = Object.values(ReportStatus);
  readonly categoriePerGrafico: ReportCategory[] = Object.values(ReportCategory);

  private canvasStatoRef?: ElementRef<HTMLCanvasElement>;
  private canvasCategoriaRef?: ElementRef<HTMLCanvasElement>;
  private canvasPrioritaRef?: ElementRef<HTMLCanvasElement>;
  private canvasMensileRef?: ElementRef<HTMLCanvasElement>;
  private canvasTeamRef?: ElementRef<HTMLCanvasElement>;

  private graficoStato?: Chart;
  private graficoCategoria?: Chart;
  private graficoPriorita?: Chart;
  private graficoMensile?: Chart;
  private graficoTeam?: Chart;

  // I canvas compaiono/spariscono col blocco @if della scheda Statistiche: un
  // ViewChild a setter, a differenza di uno statico, si accorge di entrambi i
  // casi e ridisegna il grafico appena l'elemento torna disponibile.
  @ViewChild('canvasStato') set canvasStato(ref: ElementRef<HTMLCanvasElement> | undefined) {
    this.canvasStatoRef = ref;
    if (ref) this.disegnaGraficoStato();
  }
  @ViewChild('canvasCategoria') set canvasCategoria(ref: ElementRef<HTMLCanvasElement> | undefined) {
    this.canvasCategoriaRef = ref;
    if (ref) this.disegnaGraficoCategoria();
  }
  @ViewChild('canvasPriorita') set canvasPriorita(ref: ElementRef<HTMLCanvasElement> | undefined) {
    this.canvasPrioritaRef = ref;
    if (ref) this.disegnaGraficoPriorita();
  }
  @ViewChild('canvasMensile') set canvasMensile(ref: ElementRef<HTMLCanvasElement> | undefined) {
    this.canvasMensileRef = ref;
    if (ref) this.disegnaGraficoMensile();
  }
  @ViewChild('canvasTeam') set canvasTeam(ref: ElementRef<HTMLCanvasElement> | undefined) {
    this.canvasTeamRef = ref;
    if (ref) this.disegnaGraficoTeam();
  }

  teamSceltoPerReport: { [reportId: number]: number } = {};
  operatoreSceltoPerReport: { [reportId: number]: number } = {};
  prioritaSceltaPerReport: { [reportId: number]: ReportPriority } = {};
  salvataggioPerReport: { [reportId: number]: boolean } = {};

  private sottoscrizioneTema?: Subscription;

  constructor(
    private adminService: AdminService,
    private reportService: ReportService,
    private themeService: ThemeService,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.caricaTutto();
    this.caricaStatistiche();
    // I grafici sono bitmap su canvas: senza questo restano con i colori del
    // tema con cui sono stati disegnati anche dopo che l'utente lo cambia.
    this.sottoscrizioneTema = this.themeService.cambiamentoTema$.subscribe(() => this.disegnaTuttiIGrafici());
  }

  private caricaStatistiche(): void {
    this.caricamentoStatistiche = true;
    this.adminService.getStatistics().subscribe({
      next: (statistiche) => {
        this.statistiche = statistiche;
        this.caricamentoStatistiche = false;
        this.disegnaTuttiIGrafici();
      },
      error: () => {
        this.erroreStatistiche = 'Impossibile caricare le statistiche.';
        this.caricamentoStatistiche = false;
      }
    });
  }

  ngOnDestroy(): void {
    this.sottoscrizioneTema?.unsubscribe();
    this.graficoStato?.destroy();
    this.graficoCategoria?.destroy();
    this.graficoPriorita?.destroy();
    this.graficoMensile?.destroy();
    this.graficoTeam?.destroy();
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
  // Solo getter e filtri derivati dai dati già in memoria: nessuna chiamata
  // HTTP in questo blocco.
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
  // Apertura dialog + chiamata al service; la lista si aggiorna in locale
  // (spread dell'array) invece di ricaricare tutto da capo dopo la creazione.
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
  // Le tre tendine (team, operatore, priorità) si salvano solo per ciò che è
  // davvero cambiato, con chiamate HTTP separate in sequenza vincolata: il
  // team va assegnato prima dell'operatore, perché il backend rifiuta un
  // operatore che non appartiene ancora al team assegnato.

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

  // --- Statistiche --------------------------------------------------------
  // Un metodo disegnaGrafico... per ciascuno dei cinque grafici, tutti sullo
  // stesso schema: dati da "statistiche", colori letti dalle variabili CSS
  // del tema attivo — così seguono chiaro/scuro senza logica dedicata.
  conteggioStato(stato: ReportStatus): number {
    return this.statistiche?.byStatus[stato] ?? 0;
  }

  conteggioCategoria(categoria: ReportCategory): number {
    return this.statistiche?.byCategory[categoria] ?? 0;
  }

  conteggioPriorita(priorita: ReportPriority): number {
    return this.statistiche?.byPriority[priorita] ?? 0;
  }

  private coloreCss(variabile: string): string {
    // Il tema scuro applica la classe "dark-theme" a <body>, non a <html>:
    // leggendo le variabili da document.documentElement si otterrebbero
    // sempre i valori del tema chiaro, indipendentemente dal tema attivo.
    return getComputedStyle(document.body).getPropertyValue(variabile).trim();
  }

  // I token colore di stato usano il trattino (in-progress), l'enum usa
  // l'underscore (IN_PROGRESS): qui si fa la conversione una volta sola.
  private coloreStato(stato: ReportStatus): string {
    return this.coloreCss(`--status-${stato.toLowerCase().replace(/_/g, '-')}`);
  }

  private coloreCategoria(categoria: ReportCategory): string {
    return this.coloreCss(`--cat-${categoria.toLowerCase()}`);
  }

  private colorePriorita(priorita: ReportPriority): string {
    return this.coloreCss(`--priority-${priorita.toLowerCase()}`);
  }

  private opzioniBase(coloreTesto: string): ChartOptions {
    return {
      responsive: true,
      maintainAspectRatio: false,
      // Senza animazione il disegno è sincrono: serve per poter catturare
      // l'immagine del grafico subito dopo averlo creato, nell'export PDF.
      animation: false,
      plugins: { legend: { position: 'bottom', labels: { color: coloreTesto } } }
    };
  }

  private opzioniAsse(coloreTesto: string, coloreGriglia: string): ChartOptions {
    return {
      ...this.opzioniBase(coloreTesto),
      plugins: { legend: { display: false } },
      scales: {
        x: { ticks: { color: coloreTesto }, grid: { color: coloreGriglia } },
        y: { ticks: { color: coloreTesto }, grid: { color: coloreGriglia }, beginAtZero: true }
      }
    };
  }

  private disegnaGraficoStato(coloreTesto?: string): void {
    if (!this.canvasStatoRef || !this.statistiche) return;
    this.graficoStato?.destroy();
    this.graficoStato = new Chart(this.canvasStatoRef.nativeElement, {
      type: 'doughnut',
      data: {
        labels: this.statiPerGrafico.map(s => ETICHETTE_STATO[s] ?? s),
        datasets: [{
          data: this.statiPerGrafico.map(s => this.conteggioStato(s)),
          backgroundColor: this.statiPerGrafico.map(s => this.coloreStato(s))
        }]
      },
      options: this.opzioniBase(coloreTesto ?? this.coloreCss('--color-foreground'))
    });
  }

  private disegnaGraficoCategoria(coloreTesto?: string): void {
    if (!this.canvasCategoriaRef || !this.statistiche) return;
    this.graficoCategoria?.destroy();
    this.graficoCategoria = new Chart(this.canvasCategoriaRef.nativeElement, {
      type: 'pie',
      data: {
        labels: this.categoriePerGrafico.map(c => ETICHETTE_CATEGORIA[c] ?? c),
        datasets: [{
          data: this.categoriePerGrafico.map(c => this.conteggioCategoria(c)),
          backgroundColor: this.categoriePerGrafico.map(c => this.coloreCategoria(c))
        }]
      },
      options: this.opzioniBase(coloreTesto ?? this.coloreCss('--color-foreground'))
    });
  }

  private disegnaGraficoPriorita(coloreTesto?: string, coloreGriglia?: string): void {
    if (!this.canvasPrioritaRef || !this.statistiche) return;
    this.graficoPriorita?.destroy();
    this.graficoPriorita = new Chart(this.canvasPrioritaRef.nativeElement, {
      type: 'bar',
      data: {
        labels: this.prioritaDisponibili.map(p => ETICHETTE_PRIORITA[p] ?? p),
        datasets: [{
          label: 'Segnalazioni',
          data: this.prioritaDisponibili.map(p => this.conteggioPriorita(p)),
          backgroundColor: this.prioritaDisponibili.map(p => this.colorePriorita(p))
        }]
      },
      options: this.opzioniAsse(
        coloreTesto ?? this.coloreCss('--color-muted-foreground'),
        coloreGriglia ?? this.coloreCss('--color-border')
      )
    });
  }

  private disegnaGraficoMensile(coloreTesto?: string, coloreGriglia?: string): void {
    if (!this.canvasMensileRef || !this.statistiche) return;
    this.graficoMensile?.destroy();
    const coloreLinea = this.coloreCss('--role-operator');
    this.graficoMensile = new Chart(this.canvasMensileRef.nativeElement, {
      type: 'line',
      data: {
        labels: this.statistiche.reportsPerMonth.map(m => m.month),
        datasets: [{
          label: 'Segnalazioni',
          data: this.statistiche.reportsPerMonth.map(m => m.count),
          borderColor: coloreLinea,
          backgroundColor: `${coloreLinea}33`,
          fill: true,
          tension: 0.3
        }]
      },
      options: this.opzioniAsse(
        coloreTesto ?? this.coloreCss('--color-muted-foreground'),
        coloreGriglia ?? this.coloreCss('--color-border')
      )
    });
  }

  private disegnaGraficoTeam(coloreTesto?: string, coloreGriglia?: string): void {
    if (!this.canvasTeamRef || !this.statistiche || this.statistiche.topTeams.length === 0) return;
    this.graficoTeam?.destroy();
    const squadre = this.statistiche.topTeams;
    this.graficoTeam = new Chart(this.canvasTeamRef.nativeElement, {
      type: 'bar',
      data: {
        labels: squadre.map(t => t.teamName),
        datasets: [{
          label: 'Segnalazioni risolte',
          data: squadre.map(t => t.resolvedCount),
          backgroundColor: this.coloreCss('--role-operator')
        }]
      },
      options: { ...this.opzioniAsse(
        coloreTesto ?? this.coloreCss('--color-muted-foreground'),
        coloreGriglia ?? this.coloreCss('--color-border')
      ), indexAxis: 'y' }
    });
  }

  private disegnaTuttiIGrafici(): void {
    this.disegnaGraficoStato();
    this.disegnaGraficoCategoria();
    this.disegnaGraficoPriorita();
    this.disegnaGraficoMensile();
    this.disegnaGraficoTeam();
  }

  /**
   * Ridisegna momentaneamente tutti i grafici con colori fissi adatti alla
   * stampa (il tema scuro userebbe testo chiaro, illeggibile su una pagina
   * bianca), esegue l'esportazione, poi ripristina i colori del tema attivo.
   */
  private conGraficiStampabili(azione: () => void): void {
    this.disegnaGraficoStato(TESTO_STAMPA);
    this.disegnaGraficoCategoria(TESTO_STAMPA);
    this.disegnaGraficoPriorita(TESTO_STAMPA, GRIGLIA_STAMPA);
    this.disegnaGraficoMensile(TESTO_STAMPA, GRIGLIA_STAMPA);
    this.disegnaGraficoTeam(TESTO_STAMPA, GRIGLIA_STAMPA);

    azione();

    this.disegnaTuttiIGrafici();
  }

  private dataFileEsportazione(): string {
    return new Date().toISOString().slice(0, 10);
  }

  private scaricaBlob(blob: Blob, nomeFile: string): void {
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = nomeFile;
    link.click();
    URL.revokeObjectURL(url);
  }

  /** Un campo va tra virgolette solo se contiene il separatore, per non spezzare le colonne. */
  private campoCsv(valore: string | number): string {
    const testo = String(valore);
    return testo.includes(';') || testo.includes('"')
      ? `"${testo.replace(/"/g, '""')}"`
      : testo;
  }

  esportaCsv(): void {
    if (!this.statistiche) return;
    const s = this.statistiche;
    const righe: string[] = [];

    righe.push(this.campoCsv('Statistiche CivicFix'), '');
    righe.push(`Esportato il;${new Date().toLocaleString('it-IT')}`);
    righe.push('');
    righe.push(`Segnalazioni totali;${s.totalReports}`);
    righe.push(`Tempo medio di risoluzione (ore);${s.averageResolutionHours !== null ? s.averageResolutionHours.toFixed(1) : 'N/D'}`);
    righe.push('');

    righe.push('Per stato');
    righe.push('Stato;Conteggio');
    this.statiPerGrafico.forEach(st => righe.push(`${this.campoCsv(ETICHETTE_STATO[st] ?? st)};${this.conteggioStato(st)}`));
    righe.push('');

    righe.push('Per categoria');
    righe.push('Categoria;Conteggio');
    this.categoriePerGrafico.forEach(c => righe.push(`${this.campoCsv(ETICHETTE_CATEGORIA[c] ?? c)};${this.conteggioCategoria(c)}`));
    righe.push('');

    righe.push('Per priorità');
    righe.push('Priorità;Conteggio');
    this.prioritaDisponibili.forEach(p => righe.push(`${this.campoCsv(ETICHETTE_PRIORITA[p] ?? p)};${this.conteggioPriorita(p)}`));
    righe.push('');

    righe.push('Andamento mensile');
    righe.push('Mese;Conteggio');
    s.reportsPerMonth.forEach(m => righe.push(`${this.campoCsv(m.month)};${m.count}`));
    righe.push('');

    righe.push('Team più attivi (segnalazioni risolte)');
    righe.push('Team;Risolte');
    s.topTeams.forEach(t => righe.push(`${this.campoCsv(t.teamName)};${t.resolvedCount}`));

    // Il BOM UTF-8 evita che Excel interpreti male le lettere accentate.
    const csv = '﻿' + righe.join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    this.scaricaBlob(blob, `statistiche-civicfix-${this.dataFileEsportazione()}.csv`);
  }

  esportaPdf(): void {
    if (!this.statistiche) return;
    this.conGraficiStampabili(() => this.generaPdf());
  }

  private generaPdf(): void {
    if (!this.statistiche) return;
    const s = this.statistiche;
    const doc = new jsPDF();
    const margine = 14;
    let y = 18;

    doc.setFontSize(18);
    doc.setTextColor(20);
    doc.text('Statistiche CivicFix', margine, y);
    y += 7;

    doc.setFontSize(10);
    doc.setTextColor(100);
    doc.text(`Esportato il ${new Date().toLocaleString('it-IT')}`, margine, y);
    y += 9;

    doc.setFontSize(12);
    doc.setTextColor(20);
    doc.text(`Segnalazioni totali: ${s.totalReports}`, margine, y);
    y += 7;
    const tempoMedio = s.averageResolutionHours !== null ? `${s.averageResolutionHours.toFixed(1)} h` : 'N/D';
    doc.text(`Tempo medio di risoluzione: ${tempoMedio}`, margine, y);
    y += 9;

    const larghezza = 85;
    const altezza = 55;

    this.aggiungiGraficoAlPdf(doc, this.graficoStato, margine, y, larghezza, altezza, 'Per stato');
    this.aggiungiGraficoAlPdf(doc, this.graficoCategoria, margine + larghezza + 6, y, larghezza, altezza, 'Per categoria');
    y += altezza + 12;

    this.aggiungiGraficoAlPdf(doc, this.graficoPriorita, margine, y, larghezza, altezza, 'Per priorità');
    this.aggiungiGraficoAlPdf(doc, this.graficoMensile, margine + larghezza + 6, y, larghezza, altezza, 'Andamento mensile');
    y += altezza + 12;

    if (this.graficoTeam) {
      this.aggiungiGraficoAlPdf(doc, this.graficoTeam, margine, y, larghezza * 2 + 6, altezza, 'Team più attivi');
    }

    doc.save(`statistiche-civicfix-${this.dataFileEsportazione()}.pdf`);
  }

  private aggiungiGraficoAlPdf(
    doc: jsPDF, grafico: Chart | undefined,
    x: number, y: number, larghezza: number, altezza: number, titolo: string
  ): void {
    doc.setFontSize(10);
    doc.setTextColor(20);
    doc.text(titolo, x, y);
    if (!grafico) return;
    const immagine = grafico.toBase64Image('image/png', 1);
    doc.addImage(immagine, 'PNG', x, y + 3, larghezza, altezza);
  }
}
