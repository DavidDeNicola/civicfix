import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ReportService } from '../../../core/services/report.service';
import { AuthService } from '../../../core/services/auth.service';
import { Report, ReportPhoto, ReportStatus, Update } from '../../../core/models/report.model';
import { Role } from '../../../core/models/user.model';
import { ICONE_CATEGORIA } from '../../../core/constants/category-icons';
import { Component, ElementRef, OnInit, AfterViewInit, OnDestroy, ViewChild } from '@angular/core';
import * as L from 'leaflet';

const PHOTO_BASE_URL = 'http://localhost:8080';

@Component({
  selector: 'app-report-detail',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatCardModule, MatChipsModule, MatButtonModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatProgressSpinnerModule
  ],
  templateUrl: './report-detail.component.html',
  styleUrl: './report-detail.component.scss'
})
export class ReportDetailComponent implements OnInit, AfterViewInit, OnDestroy {
  report: Report | null = null;
  photos: ReportPhoto[] = [];
  updates: Update[] = [];
  loading: boolean = true;
  errore: string | null = null;
  @ViewChild('detailMap') mapContainer!: ElementRef<HTMLDivElement>;
  private map: L.Map | null = null;

  nuovoCommento: string = '';
  nuovoStato: ReportStatus | null = null;
  salvataggioInCorso: boolean = false;

  statiDisponibili = Object.values(ReportStatus);
  photoBaseUrl = PHOTO_BASE_URL;

  constructor(
    private route: ActivatedRoute,
    private reportService: ReportService,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const id = Number(params.get('id'));
      this.caricaDati(id);
    });
  }

  ngAfterViewInit(): void {
  }

  ngOnDestroy(): void {
    this.map?.remove();
  }
  private inizializzaMappa(): void {
    if (!this.report || this.map) return;

    this.map = L.map(this.mapContainer.nativeElement).setView(
      [this.report.latitude, this.report.longitude], 16
    );

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap contributors'
    }).addTo(this.map);

    L.marker([this.report.latitude, this.report.longitude]).addTo(this.map);
  }

  get puoGestireStato(): boolean {
    const role = this.authService.role;
    return role === Role.OPERATOR || role === Role.ADMIN;
  }

  caricaDati(id: number): void {
    this.loading = true;
    this.errore = null;

    this.reportService.findById(id).subscribe({
      next: (report) => {
        this.report = report;
        this.nuovoStato = report.status;
        this.loading = false;
        setTimeout(() => this.inizializzaMappa());
      },
      error: () => {
        this.errore = 'Segnalazione non trovata.';
        this.loading = false;
      }
    });

    this.reportService.getPhotos(id).subscribe({
      next: (photos) => this.photos = photos
    });

    this.reportService.getUpdates(id).subscribe({
      next: (updates) => this.updates = updates
    });
  }

  inviaCommento(): void {
    if (!this.nuovoCommento.trim() || !this.report) return;

    this.salvataggioInCorso = true;
    this.reportService.addComment(this.report.id, this.nuovoCommento).subscribe({
      next: (update) => {
        this.updates = [...this.updates, update];
        this.nuovoCommento = '';
        this.salvataggioInCorso = false;
      },
      error: () => {
        this.errore = 'Impossibile inviare il commento.';
        this.salvataggioInCorso = false;
      }
    });
  }

  cambiaStato(): void {
    if (!this.report || !this.nuovoStato || this.nuovoStato === this.report.status) return;

    this.salvataggioInCorso = true;
    this.reportService.changeStatus(this.report.id, this.nuovoStato).subscribe({
      next: (reportAggiornato) => {
        this.report = reportAggiornato;
        this.caricaDati(reportAggiornato.id);
        this.salvataggioInCorso = false;
      },
      error: (err) => {
        this.errore = err.status === 403
          ? 'Non sei autorizzato a modificare questa segnalazione.'
          : 'Impossibile aggiornare lo stato.';
        this.salvataggioInCorso = false;
      }
    });
  }

  get iconaCategoria(): string {
    return ICONE_CATEGORIA[this.report?.category ?? ''] ?? '📍';
  }
}
