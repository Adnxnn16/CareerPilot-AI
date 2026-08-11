import { create } from 'zustand';
import api from '@/lib/api';

interface UserProfile {
  id: string;
  userId: string;
  headline?: string;
  bio?: string;
  location?: string;
  skills: string[];
}

interface User {
  id: string;
  email: string;
  name: string;
  role: string;
  profile?: UserProfile;
}

interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  checkAuth: () => Promise<void>;
  logout: () => Promise<void>;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  isAuthenticated: false,
  isLoading: true,
  checkAuth: async () => {
    try {
      const response = await api.get('/users/me');
      set({ user: response.data, isAuthenticated: true, isLoading: false });
    } catch {
      set({ user: null, isAuthenticated: false, isLoading: false });
    }
  },
  logout: async () => {
    try {
      await api.post('/auth/logout');
      set({ user: null, isAuthenticated: false });
    } catch (error) {
      console.error('Logout failed', error);
    }
  },
}));
