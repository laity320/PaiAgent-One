import client from './client';
import type { Workflow } from '@/types/workflow';

export const workflowApi = {
  list: (): Promise<Workflow[]> => client.get('/workflows'),

  get: (id: number): Promise<Workflow> => client.get(`/workflows/${id}`),

  create: (data: { name: string; description?: string; graphJson: unknown }): Promise<number> =>
    client.post('/workflows', data),

  update: (id: number, data: { name?: string; description?: string; graphJson?: unknown }): Promise<void> =>
    client.put(`/workflows/${id}`, data),

  delete: (id: number): Promise<void> => client.delete(`/workflows/${id}`),
};
