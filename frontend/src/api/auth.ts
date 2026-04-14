import client from './client';
import type { LoginRequest, LoginResponse } from '@/types/api';

export const authApi = {
  login: (data: LoginRequest): Promise<LoginResponse> =>
    client.post('/auth/login', data),

  register: (data: LoginRequest): Promise<LoginResponse> =>
    client.post('/auth/register', data),

  me: (): Promise<{ id: number; username: string; role: string }> =>
    client.get('/auth/me'),
};
