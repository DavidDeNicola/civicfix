import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { jwtDecode } from 'jwt-decode';
import { AuthResponse, Role } from '../models/user.model';

/**
 * Stato di autenticazione lato client: token in localStorage, username e
 * ruolo decodificati dal JWT, senza bisogno di richiamare il backend.
 */


/**
 * Struttura del payload contenuto nel JWT dopo la decodifica.
 * "sub" (subject) è lo username, per convenzione standard dei JWT.
 */
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

  // Letti da tutta l'app (menu, guardie di rotta) per sapere chi è loggato
  // e con che ruolo, senza dover interrogare il backend a ogni controllo.
  username: string | null = null;
  role: Role | null = null;

  /**
   * Essendo providedIn: 'root', questo costruttore gira all'avvio dell'app.
   * Prova subito a ripristinare username e ruolo da un token già salvato in
   * un accesso precedente, così un refresh della pagina non fa perdere la sessione.
   */
  constructor(private http: HttpClient) {
    this.loadFromStorage();
  }

  /**
   * Invia le credenziali al backend. Se la risposta ha successo, contiene
   * anche il JWT: `tap` intercetta la risposta "di passaggio" — senza
   * modificarla — solo per salvare la sessione con setSession, mentre
   * l'Observable restituito resta quello originale per chi chiama login().
   */

  login(username: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${API_URL}/login`, { username, password }).pipe(
      tap(response => this.setSession(response))
    );
  }

  /**
   * Come login, ma per la registrazione: il backend crea l'utente (sempre
   * come CITIZEN, mai in base a un ruolo scelto dal client) e restituisce
   * subito un token valido — dopo la registrazione l'utente risulta già
   * loggato, senza dover fare un secondo login.
   */
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

  /**
   * Completa il recupero password: manda il token ricevuto via email
   * insieme alla nuova password. A differenza di login/register non
   * restituisce un token di sessione — l'utente dovrà rifare il login
   * con la nuova password; questo token di reset è monouso e serve solo
   * a questo scambio.
   */
  resetPassword(token: string, newPassword: string): Observable<void> {
    return this.http.post<void>(`${API_URL}/reset-password`, { token, newPassword });
  }

  /**
   * Cancella il token dal localStorage e azzera lo stato in memoria.
   * Non avvisa il backend: essendo stateless (nessuna sessione lato
   * server), non c'è nulla da invalidare lì — il token semplicemente
   * smette di essere inviato.
   */
  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    this.username = null;
    this.role = null;
  }

  /** Legge il token grezzo dal localStorage: lo usa l'interceptor per allegarlo alle richieste. */
  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  /**
   * "Loggato" qui significa solo "esiste un token salvato" — non ne
   * verifica scadenza o validità: quella la controlla il backend a ogni
   * richiesta, e l'interceptor reagisce se risponde 401.
   */
  isLoggedIn(): boolean {
    return this.getToken() !== null;
  }

  /**
   * Punto unico in cui si "entra" in sessione, chiamato sia da login che
   * da register: salva il token e aggiorna username/ruolo in memoria
   * leggendoli direttamente dalla risposta del backend — non serve
   * decodificare il JWT appena ricevuto, il backend li dà già pronti.
   */
  private setSession(response: AuthResponse): void {
    localStorage.setItem(TOKEN_KEY, response.token);
    this.username = response.username;
    this.role = response.role;
  }

  /**
   * Ripristina la sessione al riavvio dell'app leggendo il token salvato.
   * A differenza di setSession qui non ho una risposta del backend pronta:
   * ricavo username e ruolo decodificando il JWT stesso. Nota: decodifica
   * soltanto, non verifica firma né scadenza — se il token non fosse più
   * valido lo scoprirà la prima richiesta reale, gestita poi dall'interceptor.
   */
  private loadFromStorage(): void {
    const token = this.getToken();
    if (!token) return;
    const decoded = jwtDecode<TokenPayload>(token);
    this.username = decoded.sub;
    this.role = decoded.role;
  }
}
