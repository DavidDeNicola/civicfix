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
  latitude: number;
  longitude: number;
  address?: string;
  reporterUsername: string;
  assignedTeamName?: string;
  assignedOperatorUsername?: string;
  createdAt: string;
  updatedAt: string;
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
