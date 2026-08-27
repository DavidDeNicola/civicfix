import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterOutlet } from '@angular/router';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { BreakpointObserver } from '@angular/cdk/layout';
import { map, shareReplay } from 'rxjs/operators';
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
    shareReplay(1)
  );

  sidenavOpened = true;

  constructor(public authService: AuthService) {
    this.isMobile$.subscribe(isMobile => this.sidenavOpened = !isMobile);
  }

  closeOnMobile(isMobile: boolean): void {
    if (isMobile) {
      this.sidenavOpened = false;
    }
  }
}
