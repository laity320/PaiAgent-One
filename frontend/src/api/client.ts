import axios from 'axios';
import { useAuthStore } from '@/stores/authStore';

const client = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
});

client.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

client.interceptors.response.use(
  (response) => {
    const data = response.data;
    if (data.code !== 200) {
      return Promise.reject(new Error(data.msg || '请求失败'));
    }
    return data.data;
  },
  (error) => {
    if (error.response?.status === 401) {
      useAuthStore.getState().logout();
      window.location.href = '/login';
    }
    const msg = error.response?.data?.msg || error.message || '网络错误';
    return Promise.reject(new Error(msg));
  }
);

export default client;
