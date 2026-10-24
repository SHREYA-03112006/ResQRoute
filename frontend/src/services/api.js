import axios from 'axios';

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

const api = axios.create({
  baseURL: BASE_URL,
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
});

// ─── Response interceptor for unified error handling ────────────────────────
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const message = error.response?.data?.message || error.message || 'Network error';
    console.error('[ResQRoute API]', message);
    return Promise.reject(new Error(message));
  }
);

// ─── Locations ───────────────────────────────────────────────────────────────
export const getLocations = (type) =>
  api.get('/locations', { params: type ? { type } : {} }).then((r) => r.data);

export const createLocation = (location) =>
  api.post('/locations', location).then((r) => r.data);

export const deleteLocation = (id) =>
  api.delete(`/locations/${id}`).then((r) => r.data);

// ─── Road Reports ────────────────────────────────────────────────────────────
export const getReports = (params = {}) =>
  api.get('/reports', { params }).then((r) => r.data);

export const updateReportReview = (id, notes, flagged) =>
  api.patch(`/reports/${id}/review`, { notes, flagged }).then((r) => r.data);

// ─── System Status ───────────────────────────────────────────────────────────
export const getSystemStatus = () =>
  api.get('/status').then((r) => r.data);

// ─── Routes (placeholder for Phase 2) ───────────────────────────────────────
export const calculateRoute = (originId, destinationId) =>
  api.post('/routes/calculate', { originId, destinationId }).then((r) => r.data);

export default api;
