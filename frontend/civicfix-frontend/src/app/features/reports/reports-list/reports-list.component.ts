import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorIntl, MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { paginatorItaliano } from '../../../core/paginator-italiano';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { ReportService } from '../../../core/services/report.service';
import { AuthService } from '../../../core/services/auth.service';
import { Report, ReportCategory, ReportStatus } from '../../../core/models/report.model';
import { Role } from '../../../core/models/user.model';
import { ReportCardComponent } from '../../../shared/components/report-card/report-card.component';
import { ReportsMapComponent } from '../../../shared/components/reports-map/reports-map.component';
import { CategoriaPipe, StatoPipe } from '../../../core/pipes/etichette.pipe';

type Vista = 'elenco' | 'mappa';

@Component({
  selector: 'app-reports-list',
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterLink, MatProgressSpinnerModule, MatCheckboxModule,
    MatIconModule, MatPaginatorModule, ReportCardComponent, ReportsMapComponent,
    StatoPipe, CategoriaPipe
  ],
  // Fornito qui e non a livello di applicazione: così il modulo paginatore
  // resta nel chunk caricato su richiesta insieme a questa pagina.
  providers: [{ provide: MatPaginatorIntl, useFactory: paginatorItaliano }],
  templateUrl: './reports-list.component.html',
  styleUrl: './reports-list.component.scss'
})
export class ReportsListComponent implements OnInit {
  reports: Report[] = [];
  loading: boolean = true;
  errore: string | null = null;

  vista: Vista = 'elenco';

  // --- Paginazione ------------------------------------------------------
  pagina = 0;
  dimensionePagina = 10;
  dimensioniPagina = [5, 10, 25, 50];
  totaleElementi = 0;

  ricerca: string = '';
  dataDa: string = '';
  dataA: string = '';
  categorieDisponibili = Object.values(ReportCategory);
  categorieSelezionate: Set<ReportCategory> = new Set();
  statiDisponibiliFiltro: ReportStatus[] = [];
  statiSelezionati: Set<ReportStatus> = new Set();

  // --- Ricerca per vicinanza -------------------------------------------
  raggiDisponibili = [1, 2, 5, 10, 25];
  raggioKm = 2;
  posizione: { lat: number; lng: number } | null = null;
  posizioneInCorso = false;
  errorePosizione: string | null = null;

  /** Evita una richiesta a ogni tasto premuto nella casella di ricerca. */
  private ricercaDigitata = new Subject<string>();

  constructor(private reportService: ReportService, public authService: AuthService) {}

  ngOnInit(): void {
    this.statiDisponibiliFiltro = this.isAdmin
      ? Object.values(ReportStatus)
      : [ReportStatus.PENDING, ReportStatus.IN_PROGRESS];

    this.statiSelezionati = new Set(this.statiDisponibiliFiltro);

    this.ricercaDigitata
      .pipe(debounceTime(350), distinctUntilChanged())
      .subscribe(() => this.filtriCambiati());

    this.carica();
  }

  /**
   * Ogni filtro è applicato dal backend: filtrarli qui vorrebbe dire agire
   * sulla sola pagina scaricata, restituendo meno risultati del dovuto.
   */
  private carica(): void {
    this.loading = true;
    this.errore = null;

    this.reportService.search({
      page: this.pagina,
      size: this.dimensionePagina,
      categories: [...this.categorieSelezionate],
      statuses: [...this.statiSelezionati],
      title: this.ricerca,
      from: this.dataDa || undefined,
      to: this.dataA || undefined,
      lat: this.posizione?.lat,
      lng: this.posizione?.lng,
      radiusKm: this.posizione ? this.raggioKm : undefined
    }).subscribe({
      next: (response) => {
        this.reports = response.content;
        this.totaleElementi = response.totalElements;
        this.loading = false;
      },
      error: () => { this.errore = 'Impossibile caricare le segnalazioni.'; this.loading = false; }
    });
  }

  /** Cambiare un filtro riporta alla prima pagina: la vecchia posizione non ha più senso. */
  filtriCambiati(): void {
    this.pagina = 0;
    this.carica();
  }

  onRicercaDigitata(valore: string): void {
    this.ricerca = valore;
    this.ricercaDigitata.next(valore);
  }

  cambiaPagina(evento: PageEvent): void {
    this.pagina = evento.pageIndex;
    this.dimensionePagina = evento.pageSize;
    this.carica();
  }

  get isAdmin(): boolean {
    return this.authService.role === Role.ADMIN;
  }

  cambiaVista(vista: Vista): void {
    this.vista = vista;
  }

  // --- Vicino a me ------------------------------------------------------

  usaLaMiaPosizione(): void {
    this.errorePosizione = null;

    if (!navigator.geolocation) {
      this.errorePosizione = 'Il browser non supporta la geolocalizzazione.';
      return;
    }

    this.posizioneInCorso = true;

    navigator.geolocation.getCurrentPosition(
      (posizione) => {
        this.posizione = {
          lat: posizione.coords.latitude,
          lng: posizione.coords.longitude
        };
        this.posizioneInCorso = false;
        this.filtriCambiati();
      },
      (errore) => {
        this.posizioneInCorso = false;
        // Il caso più comune è il permesso negato: va detto, altrimenti
        // sembra che il pulsante non funzioni.
        this.errorePosizione = errore.code === errore.PERMISSION_DENIED
          ? 'Permesso negato: consenti l\'accesso alla posizione per usare questo filtro.'
          : 'Non è stato possibile rilevare la posizione.';
      },
      { enableHighAccuracy: false, timeout: 10000, maximumAge: 60000 }
    );
  }

  cambiaRaggio(): void {
    if (this.posizione) {
      this.filtriCambiati();
    }
  }

  azzeraPosizione(): void {
    this.posizione = null;
    this.errorePosizione = null;
    this.filtriCambiati();
  }

  // --- Filtri -----------------------------------------------------------

  toggleCategoria(categoria: ReportCategory): void {
    if (this.categorieSelezionate.has(categoria)) this.categorieSelezionate.delete(categoria);
    else this.categorieSelezionate.add(categoria);
    this.filtriCambiati();
  }

  toggleStato(stato: ReportStatus): void {
    if (this.statiSelezionati.has(stato)) this.statiSelezionati.delete(stato);
    else this.statiSelezionati.add(stato);
    this.filtriCambiati();
  }
}
