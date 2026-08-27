import { Component, ElementRef, Input, AfterViewInit, OnDestroy, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import * as L from 'leaflet';
import { Report, ReportStatus } from '../../../core/models/report.model';
import { ICONE_CATEGORIA } from '../../../core/constants/category-icons';
import { segnapostoPerCategoria } from '../../../core/constants/map-marker';
import { CategoriaPipe, PrioritaPipe, StatoPipe } from '../../../core/pipes/etichette.pipe';
import { AuthService } from '../../../core/services/auth.service';
import { ReportService } from '../../../core/services/report.service';

@Component({
  selector: 'app-report-card',
  standalone: true,
  imports: [CommonModule, RouterLink, MatIconModule, StatoPipe, PrioritaPipe, CategoriaPipe],
  templateUrl: './report-card.component.html',
  styleUrl: './report-card.component.scss'
})
export class ReportCardComponent implements AfterViewInit, OnDestroy {
  @Input({ required: true }) report!: Report;
  @ViewChild('miniMap') mapContainer!: ElementRef<HTMLDivElement>;

  private map: L.Map | null = null;
  private resizeObserver: ResizeObserver | null = null;

  votoInCorso = false;

  constructor(
    private router: Router,
    private authService: AuthService,
    private reportService: ReportService
  ) {}

  get icona(): string {
    return ICONE_CATEGORIA[this.report.category] ?? 'place';
  }

  /**
   * Il sostegno serve a misurare quante altre persone sentono il problema:
   * non ha senso sulla propria segnalazione né su una già chiusa. Il backend
   * applica le stesse regole, qui si evita solo di proporre un'azione che
   * verrebbe rifiutata.
   */
  get puoVotare(): boolean {
    return this.authService.isLoggedIn()
      && this.authService.username !== this.report.reportedUsername
      && this.report.status !== ReportStatus.RESOLVED
      && this.report.status !== ReportStatus.REJECTED;
  }

  get titoloVoto(): string {
    if (!this.authService.isLoggedIn()) return 'Accedi per sostenere questa segnalazione';
    if (this.authService.username === this.report.reportedUsername) return 'È la tua segnalazione';
    if (!this.puoVotare) return 'La segnalazione è chiusa';
    return this.report.votedByCurrentUser ? 'Togli il sostegno' : 'Sostieni questa segnalazione';
  }

  alternaVoto(): void {
    if (!this.puoVotare || this.votoInCorso) return;

    this.votoInCorso = true;
    const richiesta = this.report.votedByCurrentUser
      ? this.reportService.annullaVoto(this.report.id)
      : this.reportService.vota(this.report.id);

    richiesta.subscribe({
      next: (esito) => {
        this.report.voteCount = esito.voteCount;
        this.report.votedByCurrentUser = esito.votedByCurrentUser;
        this.votoInCorso = false;
      },
      error: () => this.votoInCorso = false
    });
  }

  open(): void {
    this.router.navigate(['/reports', this.report.id]);
  }

  ngAfterViewInit(): void {

    //mappa non interattiva in ogni card, disattivo interazione col mouse
    this.map = L.map(this.mapContainer.nativeElement, {
      dragging: false,
      zoomControl: false,
      scrollWheelZoom: false,
      doubleClickZoom: false,
      touchZoom: false,
      attributionControl: false
    }).setView([this.report.latitude, this.report.longitude], 15);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(this.map);
    L.marker([this.report.latitude, this.report.longitude], {
      icon: segnapostoPerCategoria(this.report.category)
    }).addTo(this.map);

    // The mini-map is hidden below 560px (see .scss) and the sidebar/card
    // layout can also resize it at other breakpoints. Leaflet caches pixel
    // offsets at init time and doesn't notice CSS-driven size changes on its
    // own, which is what caused tiles to render outside the card bounds.
    this.resizeObserver = new ResizeObserver(() => this.map?.invalidateSize());
    this.resizeObserver.observe(this.mapContainer.nativeElement);
  }

  ngOnDestroy(): void {
    this.resizeObserver?.disconnect();
    this.map?.remove();
  }
}
