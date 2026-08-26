import { Injectable } from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {CreateReportRequest, PagedResponse, Report, ReportPhoto, ReportStatus, Update} from '../models/report.model';

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

  findById(id: number): Observable<Report> {
    return this.http.get<Report>(`${API_URL}/${id}`);
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
}
