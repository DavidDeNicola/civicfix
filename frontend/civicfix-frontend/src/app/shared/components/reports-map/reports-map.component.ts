import {
  Component, ElementRef, Input, OnChanges, OnDestroy, SimpleChanges, ViewChild
} from '@angular/core';
import { Router } from '@angular/router';
import * as L from 'leaflet';
import { Report } from '../../../core/models/report.model';
import { segnapostoPerCategoria } from '../../../core/constants/map-marker';
import { ETICHETTE_CATEGORIA, ETICHETTE_STATO } from '../../../core/pipes/etichette.pipe';

/** Centro di ripiego quando non c'è nulla da mostrare: Lecce, come altrove. */
const CENTRO_PREDEFINITO: L.LatLngExpression = [40.3515, 18.1750];

@Component({
  selector: 'app-reports-map',
  standalone: true,
  template: '<div class="mappa" #contenitore></div>',
  styles: [`
    :host { display: block; }
    .mappa {
      height: 100%;
      width: 100%;
      border-radius: var(--radius-lg);
      overflow: hidden;
    }
  `]
})
export class ReportsMapComponent implements OnChanges, OnDestroy {
  @Input({ required: true }) reports: Report[] = [];

  /** Posizione dell'utente, quando ha attivato la ricerca per vicinanza. */
  @Input() centro: { lat: number; lng: number } | null = null;
  @Input() raggioKm: number | null = null;

  @ViewChild('contenitore', { static: true }) contenitore!: ElementRef<HTMLDivElement>;

  private map: L.Map | null = null;
  private gruppoSegnaposti = L.layerGroup();
  private cerchioRaggio: L.Circle | null = null;
  private segnapostoUtente: L.CircleMarker | null = null;
  private resizeObserver: ResizeObserver | null = null;

  constructor(private router: Router) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (!this.map) {
      this.inizializzaMappa();
    }
    if (changes['reports']) {
      this.disegnaSegnaposti();
    }
    if (changes['centro'] || changes['raggioKm']) {
      this.disegnaAreaRicerca();
    }
  }

  ngOnDestroy(): void {
    this.resizeObserver?.disconnect();
    this.map?.remove();
    this.map = null;
  }

  private inizializzaMappa(): void {
    this.map = L.map(this.contenitore.nativeElement).setView(CENTRO_PREDEFINITO, 13);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap contributors'
    }).addTo(this.map);

    this.gruppoSegnaposti.addTo(this.map);

    // Il contenitore nasce spesso a dimensione zero (pannello nascosto,
    // layout non ancora assestato): Leaflet memorizza le misure all'avvio e
    // non se ne accorge da solo.
    this.resizeObserver = new ResizeObserver(() => this.map?.invalidateSize());
    this.resizeObserver.observe(this.contenitore.nativeElement);
  }

  private disegnaSegnaposti(): void {
    if (!this.map) return;

    this.gruppoSegnaposti.clearLayers();

    for (const report of this.reports) {
      const segnaposto = L.marker([report.latitude, report.longitude], {
        icon: segnapostoPerCategoria(report.category),
        title: report.title
      });

      segnaposto.bindPopup(this.contenutoPopup(report));
      // Il popup vive fuori da Angular: il pulsante si collega qui, quando
      // l'elemento esiste davvero nel DOM.
      segnaposto.on('popupopen', (evento) => {
        const bottone = evento.popup.getElement()?.querySelector('.popup-apri');
        bottone?.addEventListener('click', () => this.router.navigate(['/reports', report.id]));
      });

      this.gruppoSegnaposti.addLayer(segnaposto);
    }

    this.inquadra();
  }

  private contenutoPopup(report: Report): string {
    const categoria = ETICHETTE_CATEGORIA[report.category] ?? report.category;
    const stato = ETICHETTE_STATO[report.status] ?? report.status;

    // I valori vengono inseriti come testo, non come HTML: titolo e indirizzo
    // sono scritti dagli utenti.
    const testo = (valore: string) =>
      valore.replace(/[&<>"']/g, c =>
        ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c] as string));

    return `
      <div class="popup-segnalazione">
        <strong>${testo(report.title)}</strong>
        <div class="popup-meta">${testo(categoria)} · ${testo(stato)}</div>
        ${report.address ? `<div class="popup-meta">${testo(report.address)}</div>` : ''}
        <button type="button" class="popup-apri">Apri la segnalazione</button>
      </div>`;
  }

  private disegnaAreaRicerca(): void {
    if (!this.map) return;

    this.cerchioRaggio?.remove();
    this.cerchioRaggio = null;
    this.segnapostoUtente?.remove();
    this.segnapostoUtente = null;

    if (!this.centro) return;

    const punto: L.LatLngExpression = [this.centro.lat, this.centro.lng];

    this.segnapostoUtente = L.circleMarker(punto, {
      radius: 6, color: '#0369a1', fillColor: '#0369a1', fillOpacity: 1, weight: 2
    }).addTo(this.map).bindTooltip('La tua posizione');

    if (this.raggioKm) {
      this.cerchioRaggio = L.circle(punto, {
        radius: this.raggioKm * 1000,
        color: '#0369a1', weight: 1, fillColor: '#0369a1', fillOpacity: 0.08
      }).addTo(this.map);
    }

    this.inquadra();
  }

  /** Inquadra tutto ciò che è disegnato: segnaposti, posizione e raggio. */
  private inquadra(): void {
    if (!this.map) return;

    const limiti = L.latLngBounds([]);
    this.gruppoSegnaposti.eachLayer(layer => {
      if (layer instanceof L.Marker) limiti.extend(layer.getLatLng());
    });
    if (this.cerchioRaggio) limiti.extend(this.cerchioRaggio.getBounds());
    else if (this.centro) limiti.extend([this.centro.lat, this.centro.lng]);

    if (limiti.isValid()) {
      this.map.fitBounds(limiti, { padding: [40, 40], maxZoom: 16 });
    } else {
      this.map.setView(CENTRO_PREDEFINITO, 13);
    }
  }
}
