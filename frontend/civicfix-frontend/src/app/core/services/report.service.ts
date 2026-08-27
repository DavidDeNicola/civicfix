import { Injectable } from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {
  CreateReportRequest,
  PagedResponse,
  Report,
  ReportCategory,
  ReportPhoto,
  ReportPriority,
  ReportStatus,
  Update,
  AssignPriorityRequest,
  VoteResponse
} from '../models/report.model';

const API_URL = 'http://localhost:8080/api/reports';

@Injectable({
  providedIn: 'root'
})
export class ReportService {

  constructor(private http: HttpClient) { }

  findAll(page: number = 0, size: number = 10): Observable<PagedResponse<Report>> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size);

    return this.http.get<PagedResponse<Report>>(API_URL, { params });
  }

  /**
   * Ricerca lato server. Il filtro per vicinanza va fatto qui e non nel
   * browser: filtrare dopo aver scaricato una pagina restituirebbe meno
   * risultati del dovuto, perché la selezione avverrebbe su un sottoinsieme
   * già tagliato dalla paginazione.
   */
  search(opzioni: {
    page?: number;
    size?: number;
    categories?: ReportCategory[];
    statuses?: ReportStatus[];
    title?: string;
    from?: string;
    to?: string;
    lat?: number;
    lng?: number;
    radiusKm?: number;
  } = {}): Observable<PagedResponse<Report>> {
    let params = new HttpParams()
      .set('page', opzioni.page ?? 0)
      .set('size', opzioni.size ?? 10);

    // Un elenco vuoto non va inviato: il backend lo interpreta come
    // "nessun vincolo", ma un parametro vuoto genererebbe un valore spurio.
    for (const categoria of opzioni.categories ?? []) {
      params = params.append('categories', categoria);
    }
    for (const stato of opzioni.statuses ?? []) {
      params = params.append('statuses', stato);
    }
    if (opzioni.title?.trim()) params = params.set('title', opzioni.title.trim());
    if (opzioni.from) params = params.set('from', opzioni.from);
    if (opzioni.to) params = params.set('to', opzioni.to);

    if (opzioni.lat != null && opzioni.lng != null && opzioni.radiusKm != null) {
      params = params
        .set('lat', opzioni.lat)
        .set('lng', opzioni.lng)
        .set('radiusKm', opzioni.radiusKm);
    }

    return this.http.get<PagedResponse<Report>>(API_URL, { params });
  }

  update(id: number, dto: CreateReportRequest): Observable<Report> {
    return this.http.put<Report>(`${API_URL}/${id}`, dto);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${API_URL}/${id}`);
  }

  findById(id: number): Observable<Report> {
    return this.http.get<Report>(`${API_URL}/${id}`);
  }

  /** Sostiene una segnalazione altrui; restituisce il conteggio aggiornato. */
  vota(reportId: number): Observable<VoteResponse> {
    return this.http.post<VoteResponse>(`${API_URL}/${reportId}/vote`, {});
  }

  annullaVoto(reportId: number): Observable<VoteResponse> {
    return this.http.delete<VoteResponse>(`${API_URL}/${reportId}/vote`);
  }

  getPhotos(id: number): Observable<ReportPhoto[]> {
    return this.http.get<ReportPhoto[]>(`${API_URL}/${id}/photos`);
  }

  getUpdates(id: number): Observable<Update[]> {
    return this.http.get<Update[]>(`${API_URL}/${id}/updates`);
  }

  changeStatus(id: number, newStatus: ReportStatus): Observable<Report> {
    return this.http.put<Report>(`${API_URL}/${id}/status`, { newStatus });
  }

  addComment(id: number, message: string): Observable<Update> {
    return this.http.post<Update>(`${API_URL}/${id}/comments`, { message });
  }

  assignTeam(reportId: number, teamId: number): Observable<Report> {
    return this.http.put<Report>(`${API_URL}/${reportId}/assign-team`, { teamId });
  }

  assignOperator(reportId: number, operatorId: number): Observable<Report> {
    return this.http.put<Report>(`${API_URL}/${reportId}/assign-operator`, { operatorId });
  }

  createReport(dto: CreateReportRequest): Observable<Report> {
    return this.http.post<Report>(API_URL, dto);
  }

  uploadPhoto(reportId: number, file: File): Observable<ReportPhoto> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ReportPhoto>(`${API_URL}/${reportId}/photos`, formData);
  }

  assignPriority(reportId: number, priority: ReportPriority): Observable<Report> {
    return this.http.put<Report>(`${API_URL}/${reportId}/priority`, { priority });
  }
}
