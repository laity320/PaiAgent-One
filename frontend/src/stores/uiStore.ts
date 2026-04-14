import { create } from 'zustand';

interface UIState {
  selectedNodeId: string | null;
  isDebugDrawerOpen: boolean;
  isConfigPanelVisible: boolean;
  selectNode: (nodeId: string | null) => void;
  toggleDebugDrawer: () => void;
  setDebugDrawerOpen: (open: boolean) => void;
}

export const useUIStore = create<UIState>((set) => ({
  selectedNodeId: null,
  isDebugDrawerOpen: false,
  isConfigPanelVisible: false,

  selectNode: (nodeId) =>
    set({
      selectedNodeId: nodeId,
      isConfigPanelVisible: nodeId !== null,
    }),

  toggleDebugDrawer: () =>
    set((state) => ({ isDebugDrawerOpen: !state.isDebugDrawerOpen })),

  setDebugDrawerOpen: (open) => set({ isDebugDrawerOpen: open }),
}));
