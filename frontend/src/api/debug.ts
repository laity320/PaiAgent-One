import client from './client';

export const debugApi = {
  execute: (data: { workflowId?: number; graphJson: unknown; input: string }): Promise<unknown> =>
    client.post('/execution/debug', data),
};

export function createDebugSSE(
  workflowId: number | null,
  graphJson: unknown,
  input: string,
  token: string
): EventSource {
  const params = new URLSearchParams();
  if (workflowId) params.set('workflowId', String(workflowId));
  params.set('input', input);
  params.set('token', token);

  const url = `/api/execution/debug/stream?${params.toString()}`;
  return new EventSource(url);
}
