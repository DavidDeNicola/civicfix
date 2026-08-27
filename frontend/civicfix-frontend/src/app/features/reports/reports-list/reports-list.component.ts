import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ReportService } from '../../../core/services/report.service';
import { AuthService } from '../../../core/services/auth.service';
import { Report } from '../../../core/models/report.model';
import { Role } from '../../../core/models/user.model';
import { ReportCardComponent } from '../../../shared/components/report-card/report-card.component';

@Component({
  selector: 'app-reports-list',
  standalone: true,
  imports: [CommonModule, RouterLink, MatProgressSpinnerModule, ReportCardComponent],
  templateUrl: './reports-list.component.html',
  styleUrl: './reports-list.component.scss'
})
export class ReportsListComponent implements OnInit {
  reports: Report[] = [];
  loading: boolean = true;
  errore: string | null = null;

  constructor(private reportService: ReportService, public authService: AuthService) {}

  ngOnInit(): void {
    this.reportService.findAll(0, 100).subscribe({
      next: (response) => { this.reports = response.content; this.loading = false; },
      error: () => { this.errore = 'Impossibile caricare le segnalazioni.'; this.loading = false; }
    });
  }

  get isAdmin(): boolean {
    return this.authService.role === Role.ADMIN;
  }

  get inCorso(): Report[] {
    return this.reports.filter(r => r.status === 'IN_PROGRESS');
  }

  get inAttesa(): Report[] {
    return this.reports.filter(r => r.status === 'PENDING');
  }
}
