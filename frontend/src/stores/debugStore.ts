import { create } from 'zustand';
import type { NodeExecutionResult } from '@/types/api';

interface DebugState {
  isRunning: boolean;
  inputText: string;
  nodeResults: NodeExecutionResult[];
  finalOutput: { type: 'text' | 'audio'; content: string } | null;
  error: string | null;

  setInputText: (text: string) => void;
  setRunning: (running: boolean) => void;
  addNodeResult: (result: NodeExecutionResult) => void;
  updateNodeResult: (nodeId: string, update: Partial<NodeExecutionResult>) => void;
  setFinalOutput: (output: { type: 'text' | 'audio'; content: string } | null) => void;
  setError: (error: string | null) => void;
  reset: () => void;
}

export const useDebugStore = create<DebugState>((set, get) => ({
  isRunning: false,
  inputText: '',
  nodeResults: [],
  finalOutput: null,
  error: null,

  setInputText: (text) => set({ inputText: text }),
  setRunning: (running) => set({ isRunning: running }),

  addNodeResult: (result) =>
    set({ nodeResults: [...get().nodeResults, result] }),

  updateNodeResult: (nodeId, update) =>
    set({
      nodeResults: get().nodeResults.map((r) =>
        r.nodeId === nodeId ? { ...r, ...update } : r
      ),
    }),

  setFinalOutput: (output) => set({ finalOutput: output }),
  setError: (error) => set({ error }),

  reset: () =>
    set({
      isRunning: false,
      nodeResults: [],
      finalOutput: null,
      error: null,
    }),
}));
