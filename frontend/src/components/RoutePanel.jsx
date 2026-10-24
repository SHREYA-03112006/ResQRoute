import React, { useState } from 'react';

/**
 * RoutePanel — Phase 2 Safe Route Planning & Dispatcher Approval interface.
 *
 * <p>Supports:
 * <ul>
 *   <li>Algorithm selection: Dijkstra (standard) vs A* (heuristic)</li>
 *   <li>Multi-route comparison (Primary vs Alternative 1 & 2)</li>
 *   <li>Distance, travel time, and hazard warnings (floods, uncertain segments)</li>
 *   <li>Dispatcher review & approval workflow (Human-in-the-loop decision support)</li>
 * </ul>
 */
export default function RoutePanel({
  origin,
  destination,
  routeData,
  selectedRouteIndex,
  onSelectRouteIndex,
  algorithm,
  onChangeAlgorithm,
  onCalculateRoute,
  calculating,
  onApproveRoute,
  onRejectRoute,
  approvalStatus,
  dispatcherNotes,
  onChangeNotes,
}) {
  const [showSegments, setShowSegments] = useState(false);

  // Helper to format minutes to "Xh Ym"
  const formatTime = (minutes) => {
    if (!minutes && minutes !== 0) return '--';
    const hrs = Math.floor(minutes / 60);
    const mins = Math.round(minutes % 60);
    if (hrs > 0) {
      return `${hrs}h ${mins}m`;
    }
    return `${mins} mins`;
  };

  // Extract all available routes
  const routes = [];
  if (routeData?.primaryRoute) {
    routes.push({
      ...routeData.primaryRoute,
      label: 'Primary (Optimal)',
      badge: 'Best Time',
      badgeColor: '#3b82f6',
    });
  }
  if (routeData?.alternativeRoutes) {
    routeData.alternativeRoutes.forEach((alt, idx) => {
      routes.push({
        ...alt,
        label: `Alternative ${idx + 1}`,
        badge: 'Detour Option',
        badgeColor: '#f59e0b',
      });
    });
  }

  const activeRoute = routes[selectedRouteIndex] || routes[0];

  return (
    <div className="panel-section">
      <div className="section-header">
        <span className="section-title">🧭 Safe Route Planning</span>
        <span
          style={{
            fontSize: 10,
            background: 'rgba(34,197,94,0.15)',
            color: '#22c55e',
            padding: '2px 8px',
            borderRadius: 10,
            fontWeight: 700,
            letterSpacing: '0.5px',
          }}
        >
          Phase 2 Active
        </span>
      </div>

      <div className="section-body">
        {/* Origin and Destination Preview */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 6, marginBottom: 12 }}>
          <div
            style={{
              padding: '6px 10px',
              borderRadius: 6,
              background: 'var(--clr-surface-2)',
              borderLeft: '3px solid #3b82f6',
              fontSize: 12,
            }}
          >
            <div style={{ fontSize: 10, color: 'var(--clr-text-muted)', textTransform: 'uppercase', letterSpacing: 0.5 }}>
              Origin Depot
            </div>
            <div style={{ fontWeight: 600, color: '#f8fafc' }}>
              {origin ? origin.name : 'None selected'}
            </div>
          </div>

          <div
            style={{
              padding: '6px 10px',
              borderRadius: 6,
              background: 'var(--clr-surface-2)',
              borderLeft: '3px solid #22c55e',
              fontSize: 12,
            }}
          >
            <div style={{ fontSize: 10, color: 'var(--clr-text-muted)', textTransform: 'uppercase', letterSpacing: 0.5 }}>
              Destination Relief Camp
            </div>
            <div style={{ fontWeight: 600, color: '#f8fafc' }}>
              {destination ? destination.name : 'None selected'}
            </div>
          </div>
        </div>

        {/* Algorithm Selector */}
        <div className="form-group" style={{ marginBottom: 10 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 4 }}>
            <label className="form-label" style={{ marginBottom: 0 }}>
              Routing Algorithm
            </label>
            <span style={{ fontSize: 10, color: 'var(--clr-text-muted)' }}>
              {algorithm === 'A_STAR' ? 'Haversine Heuristic' : 'Dijkstra Min-Cost'}
            </span>
          </div>
          <div style={{ display: 'flex', gap: 6 }}>
            <button
              type="button"
              id="algo-dijkstra"
              onClick={() => onChangeAlgorithm('DIJKSTRA')}
              style={{
                flex: 1,
                padding: '6px 8px',
                fontSize: 11,
                fontWeight: 600,
                borderRadius: 6,
                border: '1px solid',
                borderColor: algorithm === 'DIJKSTRA' ? '#3b82f6' : 'var(--clr-border)',
                background: algorithm === 'DIJKSTRA' ? 'rgba(59,130,246,0.2)' : 'var(--clr-surface-2)',
                color: algorithm === 'DIJKSTRA' ? '#60a5fa' : 'var(--clr-text-muted)',
                cursor: 'pointer',
                transition: 'all 0.15s ease',
              }}
            >
              Dijkstra
            </button>
            <button
              type="button"
              id="algo-astar"
              onClick={() => onChangeAlgorithm('A_STAR')}
              style={{
                flex: 1,
                padding: '6px 8px',
                fontSize: 11,
                fontWeight: 600,
                borderRadius: 6,
                border: '1px solid',
                borderColor: algorithm === 'A_STAR' ? '#a855f7' : 'var(--clr-border)',
                background: algorithm === 'A_STAR' ? 'rgba(168,85,247,0.2)' : 'var(--clr-surface-2)',
                color: algorithm === 'A_STAR' ? '#c084fc' : 'var(--clr-text-muted)',
                cursor: 'pointer',
                transition: 'all 0.15s ease',
              }}
            >
              A* Search
            </button>
          </div>
        </div>

        {/* Calculate Action Button */}
        <button
          id="btn-calculate-route"
          className="btn btn-primary btn-full"
          disabled={!origin || !destination || calculating}
          onClick={onCalculateRoute}
          style={{ marginBottom: 12, padding: '9px 14px' }}
        >
          {calculating ? (
            <span style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8 }}>
              <div className="loading-spinner" /> Computing Graph Routes…
            </span>
          ) : (
            '⚡ Calculate Optimal & Alternate Routes'
          )}
        </button>

        {/* Calculated Routes Section */}
        {routes.length > 0 && activeRoute && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {/* Route Selection Tabs */}
            <div style={{ display: 'flex', gap: 4, background: 'var(--clr-surface-2)', padding: 3, borderRadius: 6 }}>
              {routes.map((r, idx) => (
                <button
                  key={idx}
                  type="button"
                  id={`btn-route-tab-${idx}`}
                  onClick={() => onSelectRouteIndex(idx)}
                  style={{
                    flex: 1,
                    padding: '6px 4px',
                    fontSize: 11,
                    fontWeight: 700,
                    borderRadius: 4,
                    border: 'none',
                    background: selectedRouteIndex === idx ? 'var(--clr-surface-3)' : 'transparent',
                    color: selectedRouteIndex === idx ? '#38bdf8' : 'var(--clr-text-muted)',
                    cursor: 'pointer',
                    boxShadow: selectedRouteIndex === idx ? '0 1px 4px rgba(0,0,0,0.3)' : 'none',
                    transition: 'all 0.15s ease',
                  }}
                >
                  {idx === 0 ? 'Optimal' : `Alt ${idx}`}
                </button>
              ))}
            </div>

            {/* Active Route Summary Card */}
            <div
              style={{
                background: 'var(--clr-surface-2)',
                border: '1px solid var(--clr-border)',
                borderRadius: 8,
                padding: '10px 12px',
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
                <span style={{ fontSize: 13, fontWeight: 700, color: '#f8fafc' }}>
                  {activeRoute.label}
                </span>
                <span
                  style={{
                    fontSize: 10,
                    padding: '2px 6px',
                    borderRadius: 4,
                    background: selectedRouteIndex === 0 ? 'rgba(56,189,248,0.15)' : 'rgba(245,158,11,0.15)',
                    color: selectedRouteIndex === 0 ? '#38bdf8' : '#fbbf24',
                    fontWeight: 600,
                  }}
                >
                  {activeRoute.algorithm}
                </span>
              </div>

              {/* Metrics Grid */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8, marginBottom: 10 }}>
                <div style={{ background: 'var(--clr-surface-1)', padding: '6px 8px', borderRadius: 6 }}>
                  <div style={{ fontSize: 10, color: 'var(--clr-text-muted)', textTransform: 'uppercase' }}>
                    Distance
                  </div>
                  <div style={{ fontSize: 18, fontWeight: 800, color: '#38bdf8', fontFamily: 'JetBrains Mono, monospace' }}>
                    {activeRoute.totalDistanceKm} <span style={{ fontSize: 11, fontWeight: 500 }}>km</span>
                  </div>
                </div>

                <div style={{ background: 'var(--clr-surface-1)', padding: '6px 8px', borderRadius: 6 }}>
                  <div style={{ fontSize: 10, color: 'var(--clr-text-muted)', textTransform: 'uppercase' }}>
                    Est. Travel Time
                  </div>
                  <div style={{ fontSize: 18, fontWeight: 800, color: '#34d399', fontFamily: 'JetBrains Mono, monospace' }}>
                    {formatTime(activeRoute.estimatedTimeMinutes)}
                  </div>
                </div>
              </div>

              {/* Hazard Warnings */}
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, marginBottom: 8 }}>
                {activeRoute.floodedSegments > 0 && (
                  <span
                    style={{
                      fontSize: 10,
                      fontWeight: 600,
                      background: 'rgba(245,158,11,0.18)',
                      color: '#f59e0b',
                      padding: '3px 8px',
                      borderRadius: 12,
                      display: 'flex',
                      alignItems: 'center',
                      gap: 4,
                    }}
                  >
                    🌊 {activeRoute.floodedSegments} Flooded Segment(s)
                  </span>
                )}
                {activeRoute.uncertainSegments > 0 && (
                  <span
                    style={{
                      fontSize: 10,
                      fontWeight: 600,
                      background: 'rgba(167,139,250,0.18)',
                      color: '#a78bfa',
                      padding: '3px 8px',
                      borderRadius: 12,
                      display: 'flex',
                      alignItems: 'center',
                      gap: 4,
                    }}
                  >
                    ⚠ {activeRoute.uncertainSegments} Uncertain Segment(s)
                  </span>
                )}
                {activeRoute.hasBlockedDetour && (
                  <span
                    style={{
                      fontSize: 10,
                      fontWeight: 600,
                      background: 'rgba(59,130,246,0.18)',
                      color: '#60a5fa',
                      padding: '3px 8px',
                      borderRadius: 12,
                      display: 'flex',
                      alignItems: 'center',
                      gap: 4,
                    }}
                  >
                    🔄 Detoured around blockages
                  </span>
                )}
                {activeRoute.floodedSegments === 0 && activeRoute.uncertainSegments === 0 && (
                  <span
                    style={{
                      fontSize: 10,
                      fontWeight: 600,
                      background: 'rgba(34,197,94,0.18)',
                      color: '#4ade80',
                      padding: '3px 8px',
                      borderRadius: 12,
                      display: 'flex',
                      alignItems: 'center',
                      gap: 4,
                    }}
                  >
                    ✅ All Segments Passable
                  </span>
                )}
              </div>

              {/* Cost Assumptions Explanation */}
              <div
                style={{
                  fontSize: 10,
                  color: 'var(--clr-text-muted)',
                  background: 'var(--clr-surface-1)',
                  padding: '6px 8px',
                  borderRadius: 4,
                  lineHeight: 1.4,
                  fontFamily: 'JetBrains Mono, monospace',
                }}
              >
                ℹ️ {activeRoute.costAssumptions}
              </div>

              {/* Segments Toggle Accordion */}
              <div style={{ marginTop: 8 }}>
                <button
                  type="button"
                  onClick={() => setShowSegments(!showSegments)}
                  style={{
                    background: 'none',
                    border: 'none',
                    color: '#94a3b8',
                    fontSize: 11,
                    fontWeight: 600,
                    cursor: 'pointer',
                    padding: 0,
                    display: 'flex',
                    alignItems: 'center',
                    gap: 4,
                  }}
                >
                  {showSegments ? '▲ Hide Road Segments' : `▼ View ${activeRoute.roadSegments?.length || 0} Road Segments`}
                </button>

                {showSegments && (
                  <ol
                    style={{
                      marginTop: 6,
                      paddingLeft: 18,
                      fontSize: 11,
                      color: '#cbd5e1',
                      display: 'flex',
                      flexDirection: 'column',
                      gap: 3,
                      maxHeight: 120,
                      overflowY: 'auto',
                    }}
                  >
                    {activeRoute.roadSegments?.map((seg, sIdx) => (
                      <li key={sIdx}>{seg}</li>
                    ))}
                  </ol>
                )}
              </div>
            </div>

            {/* ═══ HUMAN-IN-THE-LOOP DISPATCHER APPROVAL WORKFLOW ═════════ */}
            <div
              style={{
                background: 'rgba(30,41,59,0.7)',
                border: '1px solid rgba(148,163,184,0.2)',
                borderRadius: 8,
                padding: '10px 12px',
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
                <span style={{ fontSize: 11, fontWeight: 700, color: '#f8fafc', display: 'flex', alignItems: 'center', gap: 5 }}>
                  🛡️ Human Decision Support
                </span>
                <span
                  style={{
                    fontSize: 9,
                    fontWeight: 700,
                    padding: '2px 6px',
                    borderRadius: 4,
                    background:
                      approvalStatus === 'APPROVED'
                        ? 'rgba(34,197,94,0.2)'
                        : approvalStatus === 'REJECTED'
                        ? 'rgba(239,68,68,0.2)'
                        : 'rgba(234,179,8,0.2)',
                    color:
                      approvalStatus === 'APPROVED'
                        ? '#4ade80'
                        : approvalStatus === 'REJECTED'
                        ? '#f87171'
                        : '#facc15',
                  }}
                >
                  {approvalStatus || 'PENDING REVIEW'}
                </span>
              </div>

              <div style={{ fontSize: 10, color: '#94a3b8', marginBottom: 8, lineHeight: 1.3 }}>
                System provides decision assistance. Real-world dispatch authority remains with human emergency commanders.
              </div>

              <div className="form-group" style={{ marginBottom: 8 }}>
                <label className="form-label" style={{ fontSize: 10 }}>
                  Dispatcher Clearance Notes
                </label>
                <input
                  type="text"
                  id="input-dispatcher-notes"
                  className="form-input"
                  style={{ fontSize: 11, padding: '5px 8px' }}
                  placeholder="e.g., SDRF Convoy Alpha cleared; escort team dispatched"
                  value={dispatcherNotes}
                  onChange={(e) => onChangeNotes(e.target.value)}
                />
              </div>

              <div style={{ display: 'flex', gap: 6 }}>
                <button
                  type="button"
                  id="btn-approve-route"
                  className="btn btn-primary"
                  style={{
                    flex: 2,
                    fontSize: 11,
                    padding: '7px 10px',
                    background: '#16a34a',
                    borderColor: '#15803d',
                  }}
                  onClick={onApproveRoute}
                >
                  ✓ Approve Route
                </button>
                <button
                  type="button"
                  id="btn-reject-route"
                  className="btn btn-secondary"
                  style={{
                    flex: 1,
                    fontSize: 11,
                    padding: '7px 8px',
                    background: 'rgba(239,68,68,0.15)',
                    color: '#f87171',
                    borderColor: 'rgba(239,68,68,0.3)',
                  }}
                  onClick={onRejectRoute}
                >
                  ✕ Reject
                </button>
              </div>

              {approvalStatus === 'APPROVED' && (
                <div
                  style={{
                    marginTop: 8,
                    padding: '6px 8px',
                    borderRadius: 4,
                    background: 'rgba(34,197,94,0.15)',
                    border: '1px solid rgba(34,197,94,0.3)',
                    fontSize: 11,
                    color: '#4ade80',
                    textAlign: 'center',
                    fontWeight: 600,
                  }}
                >
                  ✓ Route Approved & Recorded in Audit Log
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
