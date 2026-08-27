import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Report } from '../../../core/models/report.model';
import { ICONE_CATEGORIA } from '../../../core/constants/category-icons';

@Component({
  selector: 'app-report-card',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './report-card.component.html',
  styleUrl: './report-card.component.scss'
})
export class ReportCardComponent {
  @Input({ required: true }) report!: Report;

  get icona(): string {
    return ICONE_CATEGORIA[this.report.category] ?? '📍';
  }
}
