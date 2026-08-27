import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { CreateUserRequest, Team } from '../../../core/models/admin.model';
import { Role } from '../../../core/models/user.model';
import { RuoloPipe } from '../../../core/pipes/etichette.pipe';

export interface UserDialogData {
  teams: Team[];
}

@Component({
  selector: 'app-user-dialog',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatDialogModule, MatFormFieldModule,
    MatInputModule, MatSelectModule, MatButtonModule, RuoloPipe
  ],
  templateUrl: './user-dialog.component.html',
  styleUrl: './user-dialog.component.scss'
})
export class UserDialogComponent {
  ruoliDisponibili = Object.values(Role);

  utente: CreateUserRequest = {
    username: '', email: '', password: '', fullName: '', role: Role.CITIZEN, teamId: null
  };

  errore: string | null = null;

  constructor(
    private dialogRef: MatDialogRef<UserDialogComponent, CreateUserRequest>,
    @Inject(MAT_DIALOG_DATA) public data: UserDialogData
  ) {}

  get serveTeam(): boolean {
    return this.utente.role === Role.OPERATOR;
  }

  annulla(): void {
    this.dialogRef.close();
  }

  conferma(): void {
    if (!this.utente.username.trim() || !this.utente.email.trim()
      || !this.utente.password || !this.utente.fullName.trim()) {
      this.errore = 'Compila tutti i campi obbligatori.';
      return;
    }

    if (this.serveTeam && !this.utente.teamId) {
      this.errore = 'Un operatore deve essere assegnato a un team.';
      return;
    }

    // Only operators belong to a team; clear any stale selection otherwise.
    if (!this.serveTeam) {
      this.utente.teamId = null;
    }

    this.dialogRef.close(this.utente);
  }
}
