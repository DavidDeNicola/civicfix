import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { jwtDecode } from 'jwt-decode';
import { AuthResponse, Role } from '../models/user.model';

interface TokenPayload {
  sub: string;
  role: Role;
}

const API_URL = 'http://localhost:8080/api/auth';
const TOKEN_KEY = 'civicfix_token';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  username: string | null = null;
  role: Role | null = null;

  constructor(private http: HttpClient) {
    this.loadFromStorage();
  }

  login(username: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${API_URL}/login`, { username, password }).pipe(
      tap(response => this.setSession(response))
    );
  }

  register(dto: { username: string; email: string; password: string; fullName: string }): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${API_URL}/register`, dto).pipe(
      tap(response => this.setSession(response))
    );
  }

  /**
   * Avvia il recupero password. Il backend risponde allo stesso modo sia che
   * l'email esista sia che non esista, quindi la UI non può (e non deve)
   * distinguere i due casi.
   */
  forgotPassword(email: string): Observable<void> {
    return this.http.post<void>(`${API_URL}/forgot-password`, { email });
  }

  resetPassword(token: string, newPassword: string): Observable<void> {
    return this.http.post<void>(`${API_URL}/reset-password`, { token, newPassword });
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    this.username = null;
    this.role = null;
  }

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  isLoggedIn(): boolean {
    return this.getToken() !== null;
  }

  private setSession(response: AuthResponse): void {
    localStorage.setItem(TOKEN_KEY, response.token);
    this.username = response.username;
    this.role = response.role;
  }

  private loadFromStorage(): void {
    const token = this.getToken();
    if (!token) return;
    const decoded = jwtDecode<TokenPayload>(token);
    this.username = decoded.sub;
    this.role = decoded.role;
  }
}
