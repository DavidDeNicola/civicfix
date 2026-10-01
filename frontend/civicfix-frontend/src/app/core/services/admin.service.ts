import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AdminUser, CreateUserRequest, CreateTeamRequest, Team, Statistics } from '../models/admin.model';
import { environment } from '../../../environments/environment';

/**
 * Chiamate HTTP per la sezione admin: gestione utenti, team e statistiche.
 * Come ReportService, sono solo wrapper verso il backend.
 */

const USERS_URL = `${environment.apiUrl}/api/admin/users`;
const TEAMS_URL = `${environment.apiUrl}/api/admin/teams`;
const STATISTICS_URL = `${environment.apiUrl}/api/admin/statistics`;

@Injectable({ providedIn: 'root' })
export class AdminService {

  constructor(private http: HttpClient) {}

  getUsers(): Observable<AdminUser[]> {
    return this.http.get<AdminUser[]>(USERS_URL);
  }

  createUser(dto: CreateUserRequest): Observable<AdminUser> {
    return this.http.post<AdminUser>(USERS_URL, dto);
  }

  assignUserTeam(userId: number, teamId: number): Observable<AdminUser> {
    return this.http.put<AdminUser>(`${USERS_URL}/${userId}/team/${teamId}`, {});
  }

  getTeams(): Observable<Team[]> {
    return this.http.get<Team[]>(TEAMS_URL);
  }

  createTeam(dto: CreateTeamRequest): Observable<Team> {
    return this.http.post<Team>(TEAMS_URL, dto);
  }

  getStatistics(): Observable<Statistics> {
    return this.http.get<Statistics>(STATISTICS_URL);
  }
}
