import React from 'react';
import DataModeBanner from './DataModeBanner';
import CitySelector from './CitySelector';

/**
 * Header — top navigation bar for the ResQ-Route dispatcher dashboard.
 *
 * @param {Object} systemStatus - Status object from /api/status
 * @param {boolean} backendError - true if backend is unreachable
 * @param {string} selectedCityId - Currently selected city ID
 * @param {Function} onSelectCity - Handler when user selects a different city
 * @param {Object} cityIncidentCounts - Counts of incidents per city
 */
export default function Header({
  systemStatus,
  backendError,
  selectedCityId,
  onSelectCity,
  cityIncidentCounts,
}) {
  return (
    <header className="header">
      <div className="header-brand">
        <div className="header-logo">🚨</div>
        <div>
          <div className="header-title">ResQ-Route</div>
          <div className="header-subtitle">Disaster Logistics & Safe-Route Planner</div>
        </div>
      </div>

      {/* Center City Selector */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <CitySelector
          selectedCityId={selectedCityId}
          onSelectCity={onSelectCity}
          cityIncidentCounts={cityIncidentCounts}
        />
      </div>

      <div className="header-meta">
        {backendError ? (
          <div className="data-mode-banner data-mode-sample">
            ⚠ DEMO MODE — Backend Offline
          </div>
        ) : systemStatus ? (
          <>
            <DataModeBanner
              mode={systemStatus.dataMode}
              lastRefresh={systemStatus.lastRefresh}
            />
            <div style={{ fontSize: 11, color: 'var(--clr-text-muted)' }}>
              📍 {systemStatus.pilotDistrict || 'Uttarakhand Command'}
            </div>
          </>
        ) : (
          <div className="data-mode-banner data-mode-sample">
            DEMO MODE • Phase 2 Intelligence
          </div>
        )}
      </div>
    </header>
  );
}
