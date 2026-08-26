export enum Role{
  CITIZEN = 'CITIZEN',
  OPERATOR = 'OPERATOR',
  ADMIN = 'ADMIN'
}

export interface User {
  id: number;
  username: string;
  email: string;
  fullName: string;
  role: Role;
  teamId?: number;
  teamName?: string;
}

export interface AuthResponse {
  token: string;
  username: string;
  role: Role;
}
