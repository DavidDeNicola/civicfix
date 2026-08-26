import { Role } from './user.model';
import { ReportCategory } from './report.model';

export interface AdminUser {
  id: number;
  username: string;
  email: string;
  fullName: string;
  role: Role;
  teamId: number | null;
  teamName: string | null;
}

export interface CreateUserRequest {
  username: string;
  email: string;
  password: string;
  fullName: string;
  role: Role;
  teamId: number | null;
}

export interface Team {
  id: number;
  name: string;
  category: ReportCategory;
  memberCount: number;
}

export interface CreateTeamRequest {
  name: string;
  category: ReportCategory;
}
