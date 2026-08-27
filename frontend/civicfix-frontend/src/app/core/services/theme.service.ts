import { Injectable, Renderer2, RendererFactory2, Inject } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { Subject } from 'rxjs';

const THEME_KEY = 'civicfix_theme';

@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  private renderer: Renderer2;
  isDarkTheme: boolean = false;

  // Serve a chi disegna su canvas (es. i grafici Chart.js): quei colori sono
  // pixel già tracciati, non CSS, quindi non seguono da soli il cambio tema.
  private cambiamentoTema = new Subject<void>();
  readonly cambiamentoTema$ = this.cambiamentoTema.asObservable();

  constructor(
    rendererFactory: RendererFactory2,
    @Inject(DOCUMENT) private document: Document
  ) {
    this.renderer = rendererFactory.createRenderer(null, null);
    this.loadFromStorage();
  }

  toggleTheme(): void {
    this.isDarkTheme = !this.isDarkTheme;
    this.applyTheme();
    localStorage.setItem(THEME_KEY, this.isDarkTheme ? 'dark' : 'light');
    this.cambiamentoTema.next();
  }

  private applyTheme(): void {
    if (this.isDarkTheme) {
      this.renderer.addClass(this.document.body, 'dark-theme');
    } else {
      this.renderer.removeClass(this.document.body, 'dark-theme');
    }
  }

  private loadFromStorage(): void {
    const saved = localStorage.getItem(THEME_KEY);
    this.isDarkTheme = saved === 'dark';
    this.applyTheme();
  }
}
