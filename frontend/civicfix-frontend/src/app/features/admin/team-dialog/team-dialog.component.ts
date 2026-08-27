import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { CreateTeamRequest } from '../../../core/models/admin.model';
import { ReportCategory } from '../../../core/models/report.model';

@Component({
  selector: 'app-team-dialog',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatDialogModule, MatFormFieldModule,
    MatInputModule, MatSelectModule, MatButtonModule
  ],
  templateUrl: './team-dialog.component.html',
  styleUrl: './team-dialog.component.scss'
})
export class TeamDialogComponent {
  categorieDisponibili = Object.values(ReportCategory);

  team: CreateTeamRequest = { name: '', category: ReportCategory.VIABILITY };

  errore: string | null = null;

  constructor(private dialogRef: MatDialogRef<TeamDialogComponent, CreateTeamRequest>) {}

  annulla(): void {
    this.dialogRef.close();
  }

  conferma(): void {
    if (!this.team.name.trim()) {
      this.errore = 'Inserisci il nome del team.';
      return;
    }

    this.dialogRef.close(this.team);
  }
}
