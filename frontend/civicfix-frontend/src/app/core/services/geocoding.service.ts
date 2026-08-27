import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map, catchError, of } from 'rxjs';

export interface RisultatoGeocoding {
  lat: number;
  lng: number;
  indirizzo: string;
}

const NOMINATIM_URL = 'https://nominatim.openstreetmap.org';

/**
 * Geocoding via Nominatim (OpenStreetMap): converte un indirizzo digitato in
 * coordinate e viceversa. Il servizio pubblico non richiede una chiave, ma
 * chiede di non essere interrogato ad ogni tasto premuto (max ~1 richiesta al
 * secondo): per questo la ricerca è innescata solo da un click o da Invio,
 * mai da un debounce sulla digitazione.
 */
@Injectable({ providedIn: 'root' })
export class GeocodingService {
  constructor(private http: HttpClient) {}

  /** Ricerca in avanti: da testo libero a un elenco di indirizzi candidati. */
  cerca(indirizzo: string): Observable<RisultatoGeocoding[]> {
    const params = new HttpParams()
      .set('format', 'json')
      .set('q', indirizzo)
      .set('limit', '5')
      // Non esclude altri paesi, ma privilegia i risultati italiani: l'app
      // serve segnalazioni civiche su un singolo comune.
      .set('countrycodes', 'it')
      .set('addressdetails', '0');

    return this.http.get<any[]>(`${NOMINATIM_URL}/search`, { params }).pipe(
      map(risultati => risultati.map(r => ({
        lat: parseFloat(r.lat),
        lng: parseFloat(r.lon),
        indirizzo: r.display_name as string
      }))),
      // Un errore di rete o di quota non deve rompere il form: si torna
      // semplicemente "nessun risultato", come una ricerca senza esito.
      catchError(() => of([]))
    );
  }

  /** Ricerca inversa: da un punto sulla mappa a un indirizzo leggibile. */
  inversa(lat: number, lng: number): Observable<string | null> {
    const params = new HttpParams()
      .set('format', 'json')
      .set('lat', lat)
      .set('lon', lng)
      .set('zoom', '18');

    return this.http.get<any>(`${NOMINATIM_URL}/reverse`, { params }).pipe(
      map(r => (r?.display_name as string) ?? null),
      catchError(() => of(null))
    );
  }
}
