import React from 'react';

/**
 * RoutePanel — placeholder for Phase 2 route results.
 * Displays the selected origin and destination and a "Phase 2" notice.
 *
 * @param {Object} origin       - Selected origin location object
 * @param {Object} destination  - Selected destination location object
 */
export default function RoutePanel({ origin, destination }) {
  if (!origin && !destination) {
    return (
      <div className="panel-section">
        <div className="section-header">
          <span className="section-title">Suggested Route</span>
        </div>
        <div className="empty-state">
          <div className="empty-state-icon">🗺️</div>
          <div>Select a depot and relief camp to calculate a route.</div>
          <div style={{ marginTop: 6, fontSize: 11, color: 'var(--clr-text-muted)' }}>
            Route calculation available in Phase 2
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="panel-section">
      <div className="section-header">
        <span className="section-title">Suggested Route</span>
        <span
          style={{
            fontSize: 10,
            background: 'rgba(59,130,246,0.15)',
            color: 'var(--clr-primary)',
            padding: '2px 7px',
            borderRadius: 10,
            fontWeight: 700,
          }}
        >
          Phase 2
        </span>
      </div>
      <div className="route-panel">
        {origin && (
          <div className="route-card">
            <div className="route-label">Origin</div>
            <div className="route-name">{origin.name}</div>
            <div style={{ fontSize: 11, color: 'var(--clr-text-muted)', marginTop: 3 }}>
              {origin.address}
            </div>
          </div>
        )}
        {destination && (
          <div className="route-card">
            <div className="route-label">Destination</div>
            <div className="route-name">{destination.name}</div>
            <div style={{ fontSize: 11, color: 'var(--clr-text-muted)', marginTop: 3 }}>
              {destination.address}
            </div>
          </div>
        )}
        <div className="error-state" style={{ background: 'rgba(59,130,246,0.08)', borderColor: 'rgba(59,130,246,0.2)', color: 'var(--clr-primary)' }}>
          ℹ️ Dijkstra/A* routing engine will be connected in Phase 2. The selected locations are visible on the map.
        </div>
      </div>
    </div>
  );
}
