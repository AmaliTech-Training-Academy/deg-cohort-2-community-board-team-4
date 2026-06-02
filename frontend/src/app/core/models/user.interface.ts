export interface User {
  id: number;
  email: string;
  name: string;
  role: 'ADMIN' | 'USER';
  createdAt?: string;
  updatedAt?: string;
}

export interface AuthResponse {
  token: string;
  refreshToken: string;
  user: User;
}
