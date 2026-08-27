import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { ReportService } from '../../../core/services/report.service';
import { AuthService } from '../../../core/services/auth.service';
import { Report, ReportCategory, ReportStatus } from '../../../core/models/report.model';
import { Role } from '../../../core/models/user.model';
import { ReportCardComponent } from '../../../shared/components/report-card/report-card.component';

@Component({
  selector: 'app-reports-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, MatProgressSpinnerModule, MatCheckboxModule, ReportCardComponent],
  templateUrl: './reports-list.component.html',
  styleUrl: './reports-list.component.scss'
})
export class ReportsListComponent implements OnInit {
  reports: Report[] = [];
  loading: boolean = true;
  errore: string | null = null;

  ricerca: string = '';
  dataDa: string = '';
  dataA: string = '';
  categorieDisponibili = Object.values(ReportCategory);
  categorieSelezionate: Set<ReportCategory> = new Set();
  statiDisponibiliFiltro: ReportStatus[] = [];
  statiSelezionati: Set<ReportStatus> = new Set();

  constructor(private reportService: ReportService, public authService: AuthService) {}

  ngOnInit(): void {
    this.statiDisponibiliFiltro = this.isAdmin
      ? Object.values(ReportStatus)
      : [ReportStatus.PENDING, ReportStatus.IN_PROGRESS];

    this.statiSelezionati = new Set(this.statiDisponibiliFiltro);

    this.reportService.findAll(0, 100).subscribe({
      next: (response) => { this.reports = response.content; this.loading = false; },
      error: () => { this.errore = 'Impossibile caricare le segnalazioni.'; this.loading = false; }
    });
  }

  get isAdmin(): boolean {
    return this.authService.role === Role.ADMIN;
  }

  toggleCategoria(categoria: ReportCategory): void {
    if (this.categorieSelezionate.has(categoria)) this.categorieSelezionate.delete(categoria);
    else this.categorieSelezionate.add(categoria);
  }

  toggleStato(stato: ReportStatus): void {
    if (this.statiSelezionati.has(stato)) this.statiSelezionati.delete(stato);
    else this.statiSelezionati.add(stato);
  }

  private get baseVisibili(): Report[] {
    if (this.isAdmin) return this.reports;
    return this.reports.filter(r => r.status === 'PENDING' || r.status === 'IN_PROGRESS');
  }

  private get filtrati(): Report[] {
    return this.baseVisibili.filter(r => {
      const matchRicerca = !this.ricerca || r.title.toLowerCase().includes(this.ricerca.toLowerCase());
      const matchStato = this.statiSelezionati.has(r.status as ReportStatus);
      const matchCategoria = this.categorieSelezionate.size === 0 || this.categorieSelezionate.has(r.category);
      const matchDataDa = !this.dataDa || r.createdAt >= this.dataDa;
      const matchDataA = !this.dataA || r.createdAt <= this.dataA + 'T23:59:59';
      return matchRicerca && matchStato && matchCategoria && matchDataDa && matchDataA;
    });
  }

  get inCorso(): Report[] { return this.filtrati.filter(r => r.status === 'IN_PROGRESS'); }
  get inAttesa(): Report[] { return this.filtrati.filter(r => r.status === 'PENDING'); }
  get tutteFiltrate(): Report[] { return this.filtrati; }
}
