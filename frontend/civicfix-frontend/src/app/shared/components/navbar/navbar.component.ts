import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../../core/services/auth.service';
import { ThemeService } from '../../../core/services/theme.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, MatToolbarModule, MatButtonModule, MatIconModule],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss'
})
export class NavbarComponent implements OnInit {
  currentUrl: string = '';

  constructor(
    public authService: AuthService,
    public themeService: ThemeService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.currentUrl = this.router.url;

    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe(() => {
      this.currentUrl = this.router.url;
    });
  }

  get isAuthPage(): boolean {
    return this.currentUrl === '/login' || this.currentUrl === '/register';
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
