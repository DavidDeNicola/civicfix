import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ReportService } from '../../../core/services/report.service';
import { Report } from '../../../core/models/report.model';
import { RouterLink } from '@angular/router';
import {MatButton} from '@angular/material/button';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-reports-list',
  standalone: true,
  imports: [CommonModule, RouterLink, MatCardModule, MatChipsModule, MatProgressSpinnerModule, MatButton],
  templateUrl: './reports-list.component.html',
  styleUrl: './reports-list.component.scss'
})
export class ReportsListComponent implements OnInit {
  reports: Report[] = [];
  loading: boolean = true;
  errore: string | null = null;

  constructor(private reportService: ReportService, public authService: AuthService) {}

  ngOnInit(): void {
    this.reportService.findAll().subscribe({
      next: (response) => {
        this.reports = response.content;
        this.loading = false;
      },
      error: () => {
        this.errore = 'Impossibile caricare le segnalazioni.';
        this.loading = false;
      }
    });
  }
}
