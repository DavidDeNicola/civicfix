import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink, MatIconModule],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent {
  constructor(public authService: AuthService) {}

  /**
   * Da autenticati la home è raggiungibile dal logo, ma il guscio dell'app
   * mostra già marchio e navigazione: la barra pubblica con Accedi/Registrati
   * sarebbe un doppione, e la CTA deve portare alle segnalazioni.
   */
  get isLoggedIn(): boolean {
    return this.authService.isLoggedIn();
  }
}
