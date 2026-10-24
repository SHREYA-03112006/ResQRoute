import React, { useState, useEffect } from 'react';
import Header from '../components/Header';
import ResQMap from '../components/ResQMap';
import ReportCard from '../components/ReportCard';
import RoutePanel from '../components/RoutePanel';
import RouteLegend from '../components/RouteLegend';
import { getLocations, getReports, getSystemStatus } from '../services/api';

/**
 * Dashboard — main page of the ResQ-Route dispatcher interface.
 *
 * Layout:
 *  ┌─ Left (340px) ─┬─ Map ─────────────────┬─ Right (300px) ─┐
 *  │ Depot select    │ OpenStreetMap + Leaflet│ Road reports    │
 *  │ Camp select     │ Location markers       │ Confidence bars │
 *  │ Route panel     │ Report markers         │ Flagged items   │
 *  │ Legend          │                        │                 │
 *  └─────────────────┴───────────────────────┴─────────────────┘
 */
export default function Dashboard() {
  // ── State ──────────────────────────────────────────────────────────────────
  const [locations, setLocations]       = useState([]);
  const [reports, setReports]           = useState([]);
  const [systemStatus, setSystemStatus] = useState(null);
  const [origin, setOrigin]             = useState(null);
  const [destination, setDestination]   = useState(null);

  const [locLoading, setLocLoading]     = useState(true);
  const [repLoading, setRepLoading]     = useState(true);
  const [backendError, setBackendError] = useState(false);

  // ── Fetch helpers ──────────────────────────────────────────────────────────
  const fetchAll = async () => {
    try {
      const [locs, reps, status] = await Promise.all([
        getLocations(),
        getReports({ hoursBack: 48 }),
        getSystemStatus(),
      ]);
      setLocations(locs);
      setReports(reps);
      setSystemStatus(status);
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

  // ── Derived data ───────────────────────────────────────────────────────────
  const depots       = locations.filter((l) => l.type === 'DEPOT');
  const reliefCamps  = locations.filter((l) => l.type === 'RELIEF_CAMP');
  const hospitals    = locations.filter((l) => l.type === 'HOSPITAL');

  const flaggedCount = reports.filter((r) => r.flaggedForReview).length;
  const blockedCount = reports.filter((r) => r.reportedStatus === 'BLOCKED').length;
  const floodedCount = reports.filter((r) => r.reportedStatus === 'FLOODED').length;

  const handleOriginChange = (e) => {
    const loc = locations.find((l) => l.id === parseInt(e.target.value));
    setOrigin(loc || null);
  };

  const handleDestinationChange = (e) => {
    const loc = locations.find((l) => l.id === parseInt(e.target.value));
    setDestination(loc || null);
  };

  // ── Render ─────────────────────────────────────────────────────────────────
  return (
    <div className="app-shell">
      <Header systemStatus={systemStatus} backendError={backendError} />

      <div className="dashboard">
        {/* ═══ LEFT PANEL ═══════════════════════════════════════════════════ */}
        <aside className="panel-left">
          {/* Depot Selection */}
          <div className="panel-section">
            <div className="section-header">
              <span className="section-title">🏭 Route Planning</span>
            </div>
            <div className="section-body">
              <div className="form-group">
                <label className="form-label" htmlFor="select-origin">Origin Depot</label>
                {locLoading ? (
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: 'var(--clr-text-muted)', fontSize: 12 }}>
                    <div className="loading-spinner" /> Loading…
                  </div>
                ) : (
                  <select id="select-origin" className="form-select" onChange={handleOriginChange} defaultValue="">
                    <option value="" disabled>Select depot…</option>
                    {depots.map((d) => (
                      <option key={d.id} value={d.id}>{d.name}</option>
                    ))}
                  </select>
                )}
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="select-destination">Destination Relief Camp</label>
                {locLoading ? (
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: 'var(--clr-text-muted)', fontSize: 12 }}>
                    <div className="loading-spinner" /> Loading…
                  </div>
                ) : (
                  <select id="select-destination" className="form-select" onChange={handleDestinationChange} defaultValue="">
                    <option value="" disabled>Select relief camp…</option>
                    {reliefCamps.map((c) => (
                      <option key={c.id} value={c.id}>{c.name}</option>
                    ))}
                  </select>
                )}
              </div>

              <button
                id="btn-calculate-route"
                className="btn btn-primary btn-full"
                disabled={!origin || !destination}
                title={!origin || !destination ? 'Select both depot and destination first' : 'Calculate route'}
              >
                🗺️ Calculate Route
              </button>

              {!origin && !destination && (
                <div style={{ fontSize: 11, color: 'var(--clr-text-muted)', marginTop: 8, fontStyle: 'italic' }}>
                  Route engine available in Phase 2
                </div>
              )}
            </div>
          </div>

          {/* Situation Summary */}
          <div className="panel-section">
            <div className="section-header">
              <span className="section-title">📊 Situation</span>
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

          {/* Route Panel */}
          <RoutePanel origin={origin} destination={destination} />

          {/* Legend */}
          <RouteLegend />

          {/* Hospitals */}
          {hospitals.length > 0 && (
            <div className="panel-section panel-section-scrollable">
              <div className="section-header">
                <span className="section-title">🏥 Hospitals</span>
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

        {/* ═══ MAP ══════════════════════════════════════════════════════════ */}
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
          />
          <div className="map-overlay-badge">
            Uttarakhand — Chamoli / Rudraprayag pilot district
          </div>
        </main>

        {/* ═══ RIGHT PANEL ══════════════════════════════════════════════════ */}
        <aside className="panel-right">
          <div className="section-header panel-section">
            <span className="section-title">📋 Road Condition Reports</span>
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
