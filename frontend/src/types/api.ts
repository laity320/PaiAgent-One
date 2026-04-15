export interface ApiResponse<T> {
  code: number;
  msg: string;
  data: T;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  userId: number;
  username: string;
  role: string;
}

export interface UserInfo {
  id: number;
  username: string;
  role: string;
}

export interface NodeExecutionResult {
  nodeId: string;
  nodeName: string;
  status: 'pending' | 'running' | 'success' | 'error';
  inputs: Record<string, unknown> | null;
  output: string | null;
  outputType: 'text' | 'audio' | null;
  duration: number;
  progress: number | null;      // 0-100
  progressText: string | null;  // e.g., "分片 3/10"
}

export interface DebugResponse {
  status: string;
  totalDurationMs: number;
  finalOutput: {
    type: 'text' | 'audio';
    content: string;
  } | null;
}
