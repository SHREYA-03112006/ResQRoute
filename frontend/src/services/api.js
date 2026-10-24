import axios from 'axios';

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081/api';

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

// ─── Routes (Phase 2 Routing Engine) ─────────────────────────────────────────
export const calculateRoute = (params) =>
  api.post('/routes/calculate', params).then((r) => r.data);

export const getRoadNetwork = () =>
  api.get('/routes/network').then((r) => r.data);

export const saveRouteRequest = (originLocationId, destinationLocationId, routeSummaryJson, alternativeCount) =>
  api.post('/routes/requests', {
    originLocationId,
    destinationLocationId,
    routeSummaryJson,
    alternativeCount
  }).then((r) => r.data);

export const approveRouteRequest = (id, notes) =>
  api.put(`/routes/requests/${id}/approve`, { notes }).then((r) => r.data);

export const rejectRouteRequest = (id, notes) =>
  api.put(`/routes/requests/${id}/reject`, { notes }).then((r) => r.data);

export const getRouteRequests = () =>
  api.get('/routes/requests').then((r) => r.data);

export const updateEdgeStatus = (edgeId, status) =>
  api.put(`/routes/edges/${edgeId}/status`, { status }).then((r) => r.data);

export default api;
