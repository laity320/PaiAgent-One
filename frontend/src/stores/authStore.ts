import { create } from 'zustand';

interface AuthState {
  token: string | null;
  user: { id: number; username: string; role: string } | null;
  isAuthenticated: boolean;
  setAuth: (token: string, user: { id: number; username: string; role: string }) => void;
  logout: () => void;
  restoreSession: () => boolean;
}

export const useAuthStore = create<AuthState>((set) => ({
  token: null,
  user: null,
  isAuthenticated: false,

  setAuth: (token, user) => {
    localStorage.setItem('pai_token', token);
    localStorage.setItem('pai_user', JSON.stringify(user));
    set({ token, user, isAuthenticated: true });
  },

  logout: () => {
    localStorage.removeItem('pai_token');
    localStorage.removeItem('pai_user');
    set({ token: null, user: null, isAuthenticated: false });
  },

  restoreSession: () => {
    const token = localStorage.getItem('pai_token');
    const userStr = localStorage.getItem('pai_user');
    if (token && userStr) {
      try {
        const user = JSON.parse(userStr);
        set({ token, user, isAuthenticated: true });
        return true;
      } catch {
        return false;
      }
    }
    return false;
  },
}));
