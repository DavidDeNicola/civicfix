import { Component, ElementRef, Input, AfterViewInit, OnDestroy, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import * as L from 'leaflet';
import { Report } from '../../../core/models/report.model';
import { ICONE_CATEGORIA } from '../../../core/constants/category-icons';

@Component({
  selector: 'app-report-card',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './report-card.component.html',
  styleUrl: './report-card.component.scss'
})
export class ReportCardComponent implements AfterViewInit, OnDestroy {
  @Input({ required: true }) report!: Report;
  @ViewChild('miniMap') mapContainer!: ElementRef<HTMLDivElement>;

  private map: L.Map | null = null;

  get icona(): string {
    return ICONE_CATEGORIA[this.report.category] ?? '📍';
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
    L.marker([this.report.latitude, this.report.longitude]).addTo(this.map);
  }

  ngOnDestroy(): void {
    this.map?.remove();
  }
}
