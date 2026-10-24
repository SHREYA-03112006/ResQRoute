import React from 'react';
import DataModeBanner from './DataModeBanner';

/**
 * Header — top navigation bar for the ResQ-Route dispatcher dashboard.
 *
 * @param {Object} systemStatus - Status object from /api/status
 * @param {boolean} backendError - true if backend is unreachable
 */
export default function Header({ systemStatus, backendError }) {
  return (
    <header className="header">
      <div className="header-brand">
        <div className="header-logo">🚨</div>
        <div>
          <div className="header-title">ResQ-Route</div>
          <div className="header-subtitle">Disaster Logistics & Safe-Route Planner</div>
        </div>
      </div>

      <div className="header-meta">
        {backendError ? (
          <div className="data-mode-banner data-mode-sample">
            ⚠ Backend offline — check server logs
          </div>
        ) : systemStatus ? (
          <>
            <DataModeBanner
              mode={systemStatus.dataMode}
              lastRefresh={systemStatus.lastRefresh}
            />
            <div style={{ fontSize: 11, color: 'var(--clr-text-muted)' }}>
              📍 {systemStatus.pilotDistrict}
            </div>
          </>
        ) : (
          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <div className="loading-spinner" />
            <span style={{ fontSize: 11, color: 'var(--clr-text-muted)' }}>Connecting…</span>
          </div>
        )}
      </div>
    </header>
  );
}
