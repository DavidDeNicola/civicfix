export enum ReportCategory {
  VIABILITY = 'VIABILITY',
  LIGHTING = 'LIGHTING',
  WASTE = 'WASTE',
  GREEN_AREAS = 'GREEN_AREAS',
  WATER = 'WATER',
  OTHER = 'OTHER'
}

export enum ReportStatus {
  PENDING = 'PENDING',
  IN_PROGRESS = 'IN_PROGRESS',
  RESOLVED = 'RESOLVED',
  REJECTED = 'REJECTED'
}

export interface Report {
  id: number;
  title: string;
  description: string;
  category: ReportCategory;
  status: ReportStatus;
  priority: ReportPriority;
  latitude: number;
  longitude: number;
  address?: string;
  reportedUsername: string;
  assignedTeamName?: string;
  assignedOperatorUsername?: string;
  voteCount: number;
  votedByCurrentUser: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface VoteResponse {
  voteCount: number;
  votedByCurrentUser: boolean;
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ReportPhoto {
  id: number;
  url: string;
  uploadedAt: string;
}

export enum UpdateType {
  COMMENT = 'COMMENT',
  STATUS_CHANGE = 'STATUS_CHANGE'
}

export interface Update {
  id: number;
  authorUsername: string;
  type: UpdateType;
  message: string;
  oldStatus: ReportStatus | null;
  newStatus: ReportStatus | null;
  createdAt: string;
}

export interface CreateReportRequest {
  title: string;
  description: string;
  category: ReportCategory;
  latitude: number;
  longitude: number;
  address: string;
}

export enum ReportPriority {
  LOW = 'LOW',
  NORMAL = 'NORMAL',
  HIGH = 'HIGH',
  URGENT = 'URGENT'
}

export interface AssignPriorityRequest {
  priority: ReportPriority;
}
