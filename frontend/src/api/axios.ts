import axios from 'axios';

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && !window.location.pathname.includes('/login')) {
      localStorage.removeItem('token');
      // Token expirado ou inválido
    }
    return Promise.reject(error);
  }
);

export function getErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error) && error.response?.data) {
    const data = error.response.data;
    if (typeof data === 'object') {
      if (data.detail) return data.detail;
      if (data.title) return data.title;
      if (data.mensagem) return data.mensagem;
      if (data.message) return data.message;
      if (Array.isArray(data.erros) && data.erros.length > 0) {
        return data.erros.map((e: { campo?: string; mensagem?: string }) => e.mensagem || e.campo).join(', ');
      }
    }
    if (typeof data === 'string') return data;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return 'Ocorreu um erro inesperado ao se comunicar com o servidor.';
}
