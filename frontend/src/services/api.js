import axios from 'axios';

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

const api = axios.create({
  baseURL: BASE_URL,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

// ─── Response interceptor for unified error handling ────────────────────────
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const message = error.response?.data?.message || error.message || 'Network error';
    console.error('[ResQRoute API Error]', message);
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

// ─── Road Reports & TinyFish Pipeline (Phase 3) ─────────────────────────────
export const getReports = (params = {}) =>
  api.get('/reports', { params }).then((r) => r.data);

export const getReportById = (id) =>
  api.get(`/reports/${id}`).then((r) => r.data);

export const ingestReport = (rawReport) =>
  api.post('/reports/ingest', rawReport).then((r) => r.data);

export const triggerTinyFishSync = (query) =>
  api.post('/reports/tinyfish/trigger-fetch', null, { params: query ? { query } : {} }).then((r) => r.data);

export const getGraphState = () =>
  api.get('/reports/graph-state').then((r) => r.data);

export const getStaleReports = () =>
  api.get('/reports/stale').then((r) => r.data);

export const submitReportReview = (id, reviewData) =>
  api.post(`/reports/${id}/review`, reviewData).then((r) => r.data);

// ─── System Status ───────────────────────────────────────────────────────────
export const getSystemStatus = () =>
  api.get('/status').then((r) => r.data);

// ─── Routes ──────────────────────────────────────────────────────────────────
export const calculateRoute = (originId, destinationId) =>
  api.post('/routes/calculate', { originId, destinationId }).then((r) => r.data);

export default api;
