import { Component, ElementRef, Input, AfterViewInit, OnDestroy, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import * as L from 'leaflet';
import { Report } from '../../../core/models/report.model';
import { ICONE_CATEGORIA } from '../../../core/constants/category-icons';
import { segnapostoPerCategoria } from '../../../core/constants/map-marker';
import { CategoriaPipe, PrioritaPipe, StatoPipe } from '../../../core/pipes/etichette.pipe';

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

  constructor(private router: Router) {}

  get icona(): string {
    return ICONE_CATEGORIA[this.report.category] ?? 'place';
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
