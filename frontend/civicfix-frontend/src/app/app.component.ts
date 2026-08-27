import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterOutlet } from '@angular/router';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { BreakpointObserver } from '@angular/cdk/layout';
import { distinctUntilChanged, map, shareReplay } from 'rxjs/operators';
import { NavbarComponent } from './shared/components/navbar/navbar.component';
import { FooterComponent } from './shared/components/footer/footer.component';
import { AuthService } from './core/services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule, RouterOutlet, RouterLink, MatSidenavModule, MatToolbarModule,
    MatIconModule, MatButtonModule, NavbarComponent, FooterComponent
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
  private breakpointObserver = inject(BreakpointObserver);

  isMobile$ = this.breakpointObserver.observe('(max-width: 768px)').pipe(
    map(result => result.matches),
    // Aprire il drawer via hamburger fa ricalcolare lo scroll-block della
    // pagina (CDK blocca lo scroll dietro l'overlay), e questo può far
    // riemettere la query anche senza un vero cambio di breakpoint. Senza
    // distinctUntilChanged, il "subscribe" sotto forzava sidenavOpened=false
    // subito dopo l'apertura: la sidebar restava bloccata a metà animazione,
    // sempre con transform "chiusa", e i link dentro non erano cliccabili.
    distinctUntilChanged(),
    shareReplay(1)
  );

  sidenavOpened = true;

  constructor(public authService: AuthService) {
    // Il valore va applicato solo quando il breakpoint cambia davvero
    // (schermo ruotato, finestra ridimensionata oltre la soglia): qui non
    // deve mai sovrascrivere un toggle manuale fatto nel frattempo.
    this.isMobile$.subscribe(isMobile => this.sidenavOpened = !isMobile);
  }

  closeOnMobile(isMobile: boolean): void {
    if (isMobile) {
      this.sidenavOpened = false;
    }
  }
}
