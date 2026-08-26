import { Injectable } from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {PagedResponse, Report} from '../models/report.model';

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
}
