import { Component, ElementRef, AfterViewInit, OnDestroy, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import * as L from 'leaflet';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { ReportService } from '../../../core/services/report.service';
import { CreateReportRequest, ReportCategory } from '../../../core/models/report.model';
import { segnapostoPerCategoria } from '../../../core/constants/map-marker';

@Component({
  selector: 'app-create-report',
  standalone: true,
  imports: [CommonModule, FormsModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule, MatCardModule],
  templateUrl: './create-report.component.html',
  styleUrl: './create-report.component.scss'
})
export class CreateReportComponent implements AfterViewInit, OnDestroy {
  @ViewChild('mapContainer') mapContainer!: ElementRef<HTMLDivElement>;

  private map: L.Map | null = null;
  private marker: L.Marker | null = null;
  private resizeObserver: ResizeObserver | null = null;

  categorieDisponibili = Object.values(ReportCategory);

  titolo: string = '';
  descrizione: string = '';
  categoria: ReportCategory = ReportCategory.VIABILITY;
  indirizzo: string = '';
  latitudine: number | null = null;
  longitudine: number | null = null;

  fileSelezionati: File[] = [];
  salvataggioInCorso: boolean = false;
  errore: string | null = null;

  constructor(private reportService: ReportService, private router: Router) {}

  ngAfterViewInit(): void {
    this.inizializzaMappa();
  }

  ngOnDestroy(): void {
    this.resizeObserver?.disconnect();
    this.map?.remove();
  }

  private inizializzaMappa(): void {
    this.map = L.map(this.mapContainer.nativeElement).setView([40.3515, 18.1750], 13);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap contributors'
    }).addTo(this.map);

    this.map.on('click', (event: L.LeafletMouseEvent) => {
      this.impostaPosizione(event.latlng.lat, event.latlng.lng);
    });

    // Keep Leaflet's cached pixel offsets in sync with responsive layout changes.
    this.resizeObserver = new ResizeObserver(() => this.map?.invalidateSize());
    this.resizeObserver.observe(this.mapContainer.nativeElement);
  }

  private impostaPosizione(lat: number, lng: number): void {
    this.latitudine = lat;
    this.longitudine = lng;

    if (this.marker) {
      this.marker.setLatLng([lat, lng]);
    } else {
      this.marker = L.marker([lat, lng], {
        icon: segnapostoPerCategoria(this.categoria)
      }).addTo(this.map!);
    }
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files) {
      this.fileSelezionati = Array.from(input.files);
    }
  }

  invia(): void {
    this.errore = null;

    if (!this.titolo || !this.descrizione || this.latitudine === null || this.longitudine === null) {
      this.errore = 'Compila tutti i campi obbligatori e seleziona una posizione sulla mappa.';
      return;
    }

    const dto: CreateReportRequest = {
      title: this.titolo,
      description: this.descrizione,
      category: this.categoria,
      latitude: this.latitudine,
      longitude: this.longitudine,
      address: this.indirizzo
    };

    this.salvataggioInCorso = true;

    this.reportService.createReport(dto).subscribe({
      next: (report) => this.caricaFotoESpostati(report.id),
      error: () => {
        this.errore = 'Impossibile creare la segnalazione.';
        this.salvataggioInCorso = false;
      }
    });
  }

  private caricaFotoESpostati(reportId: number): void {
    if (this.fileSelezionati.length === 0) {
      this.router.navigate(['/reports', reportId]);
      return;
    }

    let completati = 0;
    const totale = this.fileSelezionati.length;

    this.fileSelezionati.forEach(file => {
      this.reportService.uploadPhoto(reportId, file).subscribe({
        next: () => { completati++; if (completati === totale) this.router.navigate(['/reports', reportId]); },
        error: () => { completati++; if (completati === totale) this.router.navigate(['/reports', reportId]); }
      });
    });
  }
}
