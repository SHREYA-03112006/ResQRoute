import React, { useState, useEffect } from 'react';
import Header from '../components/Header';
import ResQMap from '../components/ResQMap';
import ReportCard from '../components/ReportCard';
import RoutePanel from '../components/RoutePanel';
import RouteLegend from '../components/RouteLegend';
import {
  getLocations,
  getReports,
  getSystemStatus,
  calculateRoute,
  getRoadNetwork,
  saveRouteRequest,
  approveRouteRequest,
  rejectRouteRequest,
} from '../services/api';

/**
 * Dashboard — main page of the ResQ-Route dispatcher interface.
 *
 * Integrated Phase 2 capabilities:
 * - Real OpenStreetMap road network graph overlay
 * - Dijkstra and A* path calculation between Depots and Relief Camps
 * - Multi-route generation (Primary optimal + viable alternatives)
 * - Dynamic condition awareness (avoiding BLOCKED roads, penalizing FLOODED & UNCERTAIN roads)
 * - Dispatcher human-in-the-loop review and audit approval logging
 */
export default function Dashboard() {
  // ── Locations & Reports State ──────────────────────────────────────────────
  const [locations, setLocations]             = useState([]);
  const [reports, setReports]                 = useState([]);
  const [systemStatus, setSystemStatus]       = useState(null);
  const [origin, setOrigin]                   = useState(null);
  const [destination, setDestination]         = useState(null);

  // ── Phase 2 Routing State ──────────────────────────────────────────────────
  const [roadNetwork, setRoadNetwork]         = useState(null);
  const [routeData, setRouteData]             = useState(null);
  const [selectedRouteIndex, setSelectedRouteIndex] = useState(0);
  const [algorithm, setAlgorithm]             = useState('DIJKSTRA');
  const [calculating, setCalculating]         = useState(false);
  const [routingError, setRoutingError]       = useState(null);

  // ── Dispatcher Review State ────────────────────────────────────────────────
  const [currentRequestId, setCurrentRequestId] = useState(null);
  const [approvalStatus, setApprovalStatus]   = useState(null);
  const [dispatcherNotes, setDispatcherNotes] = useState('');
  const [notification, setNotification]       = useState(null);

  const [locLoading, setLocLoading]           = useState(true);
  const [repLoading, setRepLoading]           = useState(true);
  const [backendError, setBackendError]       = useState(false);

  // ── Fetch Initial Data ─────────────────────────────────────────────────────
  const fetchAll = async () => {
    try {
      const [locs, reps, status, network] = await Promise.all([
        getLocations(),
        getReports({ hoursBack: 48 }),
        getSystemStatus(),
        getRoadNetwork().catch((e) => {
          console.warn('Network layer fetch failed:', e);
          return null;
        }),
      ]);
      setLocations(locs);
      setReports(reps);
      setSystemStatus(status);
      if (network) setRoadNetwork(network);
      setBackendError(false);
    } catch (err) {
      console.error('Backend error:', err.message);
      setBackendError(true);
    } finally {
      setLocLoading(false);
      setRepLoading(false);
    }
  };

  useEffect(() => {
    fetchAll();
    const interval = setInterval(fetchAll, 60000);
    return () => clearInterval(interval);
  }, []);

  // ── Calculate Route Handler ────────────────────────────────────────────────
  const handleCalculateRoute = async () => {
    if (!origin || !destination) return;

    setCalculating(true);
    setRoutingError(null);
    setApprovalStatus('PENDING');

    try {
      const data = await calculateRoute({
        originLocationId: origin.id,
        destinationLocationId: destination.id,
        algorithm,
        maxAlternatives: 2,
      });

      setRouteData(data);
      setSelectedRouteIndex(0);

      // Automatically create route audit request in database
      const summaryJson = `${data.primaryRoute.totalDistanceKm} km, est. ${data.primaryRoute.estimatedTimeMinutes} mins via ${algorithm}`;
      const savedReq = await saveRouteRequest(
        origin.id,
        destination.id,
        summaryJson,
        data.alternativeRoutes ? data.alternativeRoutes.length : 0
      );
      setCurrentRequestId(savedReq.id);

      showNotification(`✓ ${algorithm} route calculated: ${data.primaryRoute.totalDistanceKm} km`);
    } catch (err) {
      console.error('Route calculation error:', err);
      setRoutingError(err.message || 'Failed to calculate route. Check road network connectivity.');
      showNotification(`⚠ Routing failed: ${err.message}`, 'error');
    } finally {
      setCalculating(false);
    }
  };

  // ── Dispatcher Review Handlers ─────────────────────────────────────────────
  const handleApproveRoute = async () => {
    if (!currentRequestId) {
      showNotification('Please calculate a route first before approving.', 'error');
      return;
    }
    try {
      await approveRouteRequest(currentRequestId, dispatcherNotes || 'Approved by dispatcher');
      setApprovalStatus('APPROVED');
      showNotification('✓ Route approved and recorded in audit log.');
    } catch (err) {
      console.error('Failed to approve route:', err);
      showNotification('Error approving route: ' + err.message, 'error');
    }
  };

  const handleRejectRoute = async () => {
    if (!currentRequestId) return;
    try {
      await rejectRouteRequest(currentRequestId, dispatcherNotes || 'Rejected by dispatcher');
      setApprovalStatus('REJECTED');
      showNotification('Route suggestion rejected by dispatcher.');
    } catch (err) {
      console.error('Failed to reject route:', err);
      showNotification('Error rejecting route: ' + err.message, 'error');
    }
  };

  const showNotification = (msg, type = 'info') => {
    setNotification({ msg, type });
    setTimeout(() => setNotification(null), 5000);
  };

  // ── Derived Location Categories ────────────────────────────────────────────
  const depots       = locations.filter((l) => l.type === 'DEPOT');
  const reliefCamps  = locations.filter((l) => l.type === 'RELIEF_CAMP');
  const hospitals    = locations.filter((l) => l.type === 'HOSPITAL');

  const flaggedCount = reports.filter((r) => r.flaggedForReview).length;
  const blockedCount = reports.filter((r) => r.reportedStatus === 'BLOCKED').length;
  const floodedCount = reports.filter((r) => r.reportedStatus === 'FLOODED').length;

  const handleOriginChange = (e) => {
    const loc = locations.find((l) => l.id === parseInt(e.target.value));
    setOrigin(loc || null);
    setRouteData(null);
    setApprovalStatus(null);
  };

  const handleDestinationChange = (e) => {
    const loc = locations.find((l) => l.id === parseInt(e.target.value));
    setDestination(loc || null);
    setRouteData(null);
    setApprovalStatus(null);
  };

  return (
    <div className="app-shell">
      <Header systemStatus={systemStatus} backendError={backendError} />

      {/* Floating Notification Toast */}
      {notification && (
        <div
          style={{
            position: 'fixed',
            top: 60,
            right: 24,
            zIndex: 9999,
            background: notification.type === 'error' ? 'rgba(239,68,68,0.92)' : 'rgba(34,197,94,0.92)',
            color: '#ffffff',
            padding: '10px 18px',
            borderRadius: 8,
            fontWeight: 600,
            fontSize: 13,
            boxShadow: '0 4px 16px rgba(0,0,0,0.3)',
            backdropFilter: 'blur(8px)',
            transition: 'all 0.3s ease',
          }}
        >
          {notification.msg}
        </div>
      )}

      <div className="dashboard">
        {/* ═══ LEFT PANEL ═══════════════════════════════════════════════════ */}
        <aside className="panel-left">
          {/* Depot & Camp Selection */}
          <div className="panel-section">
            <div className="section-header">
              <span className="section-title">🏭 Origin & Destination</span>
            </div>
            <div className="section-body">
              <div className="form-group">
                <label className="form-label" htmlFor="select-origin">Origin Relief Depot</label>
                {locLoading ? (
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: 'var(--clr-text-muted)', fontSize: 12 }}>
                    <div className="loading-spinner" /> Loading depots…
                  </div>
                ) : (
                  <select id="select-origin" className="form-select" onChange={handleOriginChange} defaultValue="">
                    <option value="" disabled>Select origin depot…</option>
                    {depots.map((d) => (
                      <option key={d.id} value={d.id}>{d.name}</option>
                    ))}
                  </select>
                )}
              </div>

              <div className="form-group" style={{ marginBottom: 4 }}>
                <label className="form-label" htmlFor="select-destination">Destination Relief Camp</label>
                {locLoading ? (
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: 'var(--clr-text-muted)', fontSize: 12 }}>
                    <div className="loading-spinner" /> Loading camps…
                  </div>
                ) : (
                  <select id="select-destination" className="form-select" onChange={handleDestinationChange} defaultValue="">
                    <option value="" disabled>Select destination camp…</option>
                    {reliefCamps.map((c) => (
                      <option key={c.id} value={c.id}>{c.name}</option>
                    ))}
                  </select>
                )}
              </div>

              {routingError && (
                <div
                  style={{
                    marginTop: 8,
                    padding: '8px 10px',
                    borderRadius: 6,
                    background: 'rgba(239,68,68,0.15)',
                    border: '1px solid rgba(239,68,68,0.3)',
                    color: '#f87171',
                    fontSize: 11,
                  }}
                >
                  ⚠ {routingError}
                </div>
              )}
            </div>
          </div>

          {/* Phase 2 Safe Route Planning Panel */}
          <RoutePanel
            origin={origin}
            destination={destination}
            routeData={routeData}
            selectedRouteIndex={selectedRouteIndex}
            onSelectRouteIndex={setSelectedRouteIndex}
            algorithm={algorithm}
            onChangeAlgorithm={setAlgorithm}
            onCalculateRoute={handleCalculateRoute}
            calculating={calculating}
            onApproveRoute={handleApproveRoute}
            onRejectRoute={handleRejectRoute}
            approvalStatus={approvalStatus}
            dispatcherNotes={dispatcherNotes}
            onChangeNotes={setDispatcherNotes}
          />

          {/* District Situation Summary */}
          <div className="panel-section">
            <div className="section-header">
              <span className="section-title">📊 District Hazards</span>
            </div>
            <div className="section-body" style={{ display: 'flex', gap: 8 }}>
              {[
                { label: 'Blocked', value: blockedCount, color: 'var(--clr-blocked)' },
                { label: 'Flooded', value: floodedCount, color: 'var(--clr-flooded)' },
                { label: 'Flagged', value: flaggedCount, color: 'var(--clr-uncertain)' },
              ].map((item) => (
                <div
                  key={item.label}
                  style={{
                    flex: 1,
                    background: 'var(--clr-surface-2)',
                    border: '1px solid var(--clr-border)',
                    borderRadius: 'var(--radius-sm)',
                    padding: '8px 6px',
                    textAlign: 'center',
                  }}
                >
                  <div style={{ fontSize: 20, fontWeight: 800, color: item.color, fontFamily: 'JetBrains Mono, monospace' }}>
                    {item.value}
                  </div>
                  <div style={{ fontSize: 10, color: 'var(--clr-text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                    {item.label}
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Map Legend */}
          <RouteLegend />

          {/* District Hospitals */}
          {hospitals.length > 0 && (
            <div className="panel-section panel-section-scrollable">
              <div className="section-header">
                <span className="section-title">🏥 Emergency Hospitals</span>
                <span className="section-count">{hospitals.length}</span>
              </div>
              <div className="section-body">
                {hospitals.map((h) => (
                  <div key={h.id} style={{ marginBottom: 6, fontSize: 12 }}>
                    <div style={{ fontWeight: 600 }}>{h.name}</div>
                    <div style={{ color: 'var(--clr-text-muted)', fontSize: 11 }}>{h.address}</div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </aside>

        {/* ═══ INTERACTIVE LEAFLET MAP ══════════════════════════════════════ */}
        <main className="map-container">
          {backendError && (
            <div style={{
              position: 'absolute', top: 16, left: '50%', transform: 'translateX(-50%)',
              zIndex: 500, background: 'rgba(239,68,68,0.15)', border: '1px solid rgba(239,68,68,0.4)',
              borderRadius: 'var(--radius-sm)', padding: '8px 16px', fontSize: 12, color: '#fca5a5',
            }}>
              ⚠ Backend unavailable — map shows cached/sample data only
            </div>
          )}
          <ResQMap
            locations={locations}
            reports={reports}
            origin={origin}
            destination={destination}
            roadNetwork={roadNetwork}
            routeData={routeData}
            selectedRouteIndex={selectedRouteIndex}
            onSelectRouteIndex={setSelectedRouteIndex}
          />
          <div className="map-overlay-badge">
            Uttarakhand — Chamoli / Rudraprayag Road Network Engine
          </div>
        </main>

        {/* ═══ RIGHT PANEL: ROAD REPORTS ════════════════════════════════════ */}
        <aside className="panel-right">
          <div className="section-header panel-section">
            <span className="section-title">📋 Public Road Reports</span>
            <span className="section-count">{reports.length}</span>
          </div>

          {repLoading ? (
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 10, padding: 24, color: 'var(--clr-text-muted)', fontSize: 13 }}>
              <div className="loading-spinner" /> Loading reports…
            </div>
          ) : reports.length === 0 ? (
            <div className="empty-state">
              <div className="empty-state-icon">📭</div>
              <div>No road-condition reports found.</div>
            </div>
          ) : (
            <div className="panel-section-scrollable" style={{ padding: '8px 12px' }}>
              {/* Flagged reports first */}
              {reports.filter((r) => r.flaggedForReview).length > 0 && (
                <>
                  <div style={{ fontSize: 10, fontWeight: 700, textTransform: 'uppercase', letterSpacing: 1, color: 'var(--clr-flooded)', margin: '4px 0 8px' }}>
                    ⚑ Requires Review
                  </div>
                  {reports.filter((r) => r.flaggedForReview).map((r) => (
                    <ReportCard key={r.id} report={r} />
                  ))}
                  <div style={{ height: 8 }} />
                </>
              )}

              {/* Blocked roads */}
              {reports.filter((r) => r.reportedStatus === 'BLOCKED' && !r.flaggedForReview).length > 0 && (
                <>
                  <div style={{ fontSize: 10, fontWeight: 700, textTransform: 'uppercase', letterSpacing: 1, color: 'var(--clr-blocked)', margin: '4px 0 8px' }}>
                    🚫 Blocked Roads
                  </div>
                  {reports.filter((r) => r.reportedStatus === 'BLOCKED' && !r.flaggedForReview).map((r) => (
                    <ReportCard key={r.id} report={r} />
                  ))}
                  <div style={{ height: 8 }} />
                </>
              )}

              {/* Flooded roads */}
              {reports.filter((r) => r.reportedStatus === 'FLOODED' && !r.flaggedForReview).length > 0 && (
                <>
                  <div style={{ fontSize: 10, fontWeight: 700, textTransform: 'uppercase', letterSpacing: 1, color: 'var(--clr-flooded)', margin: '4px 0 8px' }}>
                    🌊 Flooded Roads
                  </div>
                  {reports.filter((r) => r.reportedStatus === 'FLOODED' && !r.flaggedForReview).map((r) => (
                    <ReportCard key={r.id} report={r} />
                  ))}
                </>
              )}
            </div>
          )}
        </aside>
      </div>
    </div>
  );
}
