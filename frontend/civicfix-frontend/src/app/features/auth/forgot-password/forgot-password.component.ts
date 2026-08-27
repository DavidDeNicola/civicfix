import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [
    FormsModule, RouterLink, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatCardModule, MatIconModule
  ],
  templateUrl: './forgot-password.component.html',
  styleUrl: './forgot-password.component.scss'
})
export class ForgotPasswordComponent {
  email: string = '';
  loading: boolean = false;
  errore: string | null = null;
  inviato: boolean = false;

  constructor(private authService: AuthService) {}

  invia(): void {
    this.errore = null;

    if (!this.email.trim()) {
      this.errore = 'Inserisci il tuo indirizzo email.';
      return;
    }

    this.loading = true;

    this.authService.forgotPassword(this.email.trim()).subscribe({
      next: () => {
        // Confermiamo sempre allo stesso modo: dire "email non trovata"
        // rivelerebbe quali indirizzi sono registrati.
        this.inviato = true;
        this.loading = false;
      },
      error: () => {
        this.errore = 'Impossibile completare la richiesta. Riprova più tardi.';
        this.loading = false;
      }
    });
  }
}
