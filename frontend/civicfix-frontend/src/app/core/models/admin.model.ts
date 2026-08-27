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

export interface MonthlyCount {
  month: string;
  count: number;
}

export interface TeamCount {
  teamName: string;
  resolvedCount: number;
}

export interface Statistics {
  totalReports: number;
  byStatus: Record<string, number>;
  byCategory: Record<string, number>;
  byPriority: Record<string, number>;
  reportsPerMonth: MonthlyCount[];
  averageResolutionHours: number | null;
  topTeams: TeamCount[];
}
