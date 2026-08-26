import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatCardModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  username: string = '';
  password: string = '';
  loading: boolean = false;
  errore: string | null = null;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  login(): void {
    if (!this.username || !this.password) {
      this.errore = 'Compila tutti i campi.';
      return;
    }

    this.loading = true;
    this.errore = null;

    this.authService.login(this.username, this.password).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigateByUrl('/reports');
      },
      error: (err) => {
        this.loading = false;
        if (err.status === 401) {
          this.errore = 'Username o password non corretti.';
        } else {
          this.errore = 'Errore durante il login. Riprova più tardi.';
        }
      }
    });
  }
}
