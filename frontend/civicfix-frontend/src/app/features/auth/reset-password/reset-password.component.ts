import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../../core/services/auth.service';

const LUNGHEZZA_MINIMA = 8;

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [
    FormsModule, RouterLink, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatCardModule, MatIconModule
  ],
  templateUrl: './reset-password.component.html',
  styleUrl: './reset-password.component.scss'
})
export class ResetPasswordComponent implements OnInit {
  token: string | null = null;

  password: string = '';
  conferma: string = '';
  mostraPassword: boolean = false;

  loading: boolean = false;
  errore: string | null = null;
  completato: boolean = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token');
  }

  invia(): void {
    this.errore = null;

    if (!this.token) return;

    if (this.password.length < LUNGHEZZA_MINIMA) {
      this.errore = `La password deve avere almeno ${LUNGHEZZA_MINIMA} caratteri.`;
      return;
    }

    if (this.password !== this.conferma) {
      this.errore = 'Le due password non coincidono.';
      return;
    }

    this.loading = true;

    this.authService.resetPassword(this.token, this.password).subscribe({
      next: () => {
        this.completato = true;
        this.loading = false;
        setTimeout(() => this.router.navigate(['/login']), 2500);
      },
      error: (err) => {
        // Il backend distingue link non valido da link scaduto: riportiamo il
        // suo messaggio, che dice all'utente cosa fare.
        this.errore = err.error?.message
          ?? 'Impossibile reimpostare la password. Richiedi un nuovo link.';
        this.loading = false;
      }
    });
  }
}
