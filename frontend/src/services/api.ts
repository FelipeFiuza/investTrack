import axios from 'axios';
import { Usuario, UsuarioCreateDTO, Transacao, TransacaoCreateDTO, ApuracaoIndice, TipoInvestimento, Indice } from '../types';

const API_BASE_URL = 'http://localhost:6868/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Usuario API
export const usuarioApi = {
  getAll: (nome?: string) => api.get<Usuario[]>(`/usuarios${nome ? `?nome=${nome}` : ''}`),
  getById: (id: number) => api.get<Usuario>(`/usuarios/${id}`),
  create: (data: UsuarioCreateDTO) => api.post<Usuario>('/usuarios', data),
  update: (id: number, data: Partial<Usuario>) => api.put<Usuario>(`/usuarios/${id}`, data),
  delete: (id: number) => api.delete(`/usuarios/${id}`),
  getByToken: (token: string) => api.get<Usuario>(`/usuarios/token/${token}`),
};

// Transacao API (assuming endpoints will be created)
export const transacaoApi = {
  getAll: (idUsuario?: number) => api.get<Transacao[]>(`/transacoes${idUsuario ? `?idUsuario=${idUsuario}` : ''}`),
  getById: (id: number) => api.get<Transacao>(`/transacoes/${id}`),
  create: (data: TransacaoCreateDTO) => api.post<Transacao>('/transacoes', data),
  update: (id: number, data: Partial<Transacao>) => api.put<Transacao>(`/transacoes/${id}`, data),
  delete: (id: number) => api.delete(`/transacoes/${id}`),
};

// ApuracaoIndice API
export const apuracaoIndiceApi = {
  getAll: (inicio?: string, fim?: string) => 
    api.get<ApuracaoIndice[]>(`/apuracoes-indice${inicio || fim ? `?inicio=${inicio}&fim=${fim}` : ''}`),
  getById: (id: number) => api.get<ApuracaoIndice>(`/apuracoes-indice/${id}`),
};

// TipoInvestimento API
export const tipoInvestimentoApi = {
  getAll: () => api.get<TipoInvestimento[]>('/tipos-investimento'),
  getById: (id: number) => api.get<TipoInvestimento>(`/tipos-investimento/${id}`),
};

// Indice API
export const indiceApi = {
  getAll: (descricao?: string, tipoIndice?: string) => 
    api.get<Indice[]>(`/indices${descricao || tipoIndice ? `?descricao=${descricao}&tipoIndice=${tipoIndice}` : ''}`),
  getById: (id: number) => api.get<Indice>(`/indices/${id}`),
};

export default api;
