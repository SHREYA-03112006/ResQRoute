import React, { useState, useEffect, useMemo, useCallback } from 'react';
import Header from '../components/Header';
import ResQMap from '../components/ResQMap';
import ReportCard from '../components/ReportCard';
import RoutePanel from '../components/RoutePanel';
import RouteLegend from '../components/RouteLegend';
import LiveLocationControl from '../components/LiveLocationControl';
import { getCities, getCityById, DEFAULT_CITY_ID } from '../data/cityData';
import { useLiveLocation } from '../hooks/useLiveLocation';
import { getLocations, getReports, getSystemStatus, triggerTinyFishSync, submitReportReview } from '../services/api';

/**
 * Dashboard — ResQRoute Emergency Disaster Response & Rescue Coordination Dashboard.
 *
 * Phase 1 features:
 *  - City-wise interactive map with road network and Google Maps integration
 *  - Configurable city selector (Dehradun default, Rishikesh, Haridwar, Rudraprayag, Chamoli, Nainital)
 *  - Clear affected disaster hazard perimeters and road blockage markers (all clearly labeled DEMO)
 *  - Live rescue-team location tracking using browser Geolocation API with user authorization
 *  - Safe fallback map engine when Google Maps API key is not configured
 */
export default function Dashboard() {
  // ── City & Sector State ────────────────────────────────────────────────────
  const [selectedCityId, setSelectedCityId] = useState(DEFAULT_CITY_ID);
  const selectedCity = useMemo(() => getCityById(selectedCityId), [selectedCityId]);

  // ── Live Geolocation Hook ──────────────────────────────────────────────────
  const liveLocation = useLiveLocation();

  // ── Backend / Data State ───────────────────────────────────────────────────
  const [backendLocations, setBackendLocations] = useState([]);
  const [backendReports, setBackendReports]     = useState([]);
  const [systemStatus, setSystemStatus]         = useState(null);
  const [backendError, setBackendError]         = useState(false);
  const [loadingBackend, setLoadingBackend]     = useState(true);

  // ── Routing & Selection State ──────────────────────────────────────────────
  const [origin, setOrigin]                     = useState(null);
  const [destination, setDestination]           = useState(null);
  const [selectedIncident, setSelectedIncident] = useState(null);
  const [activeFilter, setActiveFilter]         = useState('ALL'); // 'ALL' | 'CRITICAL' | 'BLOCKED' | 'FLOODED' | 'OPEN'

  // TinyFish sync status
  const [syncingTinyFish, setSyncing]           = useState(false);
  const [syncSuccessMsg, setSyncMsg]            = useState('');

  // ── Fetch Backend Data ─────────────────────────────────────────────────────
  const fetchBackend = useCallback(async () => {
    try {
      const [locs, reps, status] = await Promise.all([
        getLocations().catch(() => []),
        getReports({ hoursBack: 48 }).catch(() => []),
        getSystemStatus().catch(() => null),
      ]);
      setBackendLocations(Array.isArray(locs) ? locs : []);
      setBackendReports(Array.isArray(reps) ? reps : []);
      if (status) setSystemStatus(status);
      setBackendError(false);
    } catch {
      setBackendError(true);
    } finally {
      setLoadingBackend(false);
    }
  }, []);

  useEffect(() => {
    fetchBackend();
  }, [fetchBackend]);

  // Reset origin/destination when city changes
  useEffect(() => {
    setOrigin(null);
    setDestination(null);
    setSelectedIncident(null);
  }, [selectedCityId]);

  // ── Unified Data Resolution ───────────────────────────────────────────────
  // Combine city demo data with backend data (if available)
  const currentLocations = useMemo(() => {
    const cityLocs = selectedCity.locations || [];
    if (backendLocations.length > 0) {
      // Merge unique backend locations
      return [...cityLocs, ...backendLocations.filter((bl) => !cityLocs.some((cl) => cl.id === bl.id))];
    }
    return cityLocs;
  }, [selectedCity, backendLocations]);

  const currentIncidents = useMemo(() => {
    const cityIncs = selectedCity.incidents || [];
    if (backendReports.length > 0) {
      return [...cityIncs, ...backendReports];
    }
    return cityIncs;
  }, [selectedCity, backendReports]);

  const hazardZones = useMemo(() => {
    return selectedCity.hazardZones || [];
  }, [selectedCity]);

  // Counts for cities to display in header selector
  const cityIncidentCounts = useMemo(() => {
    const counts = {};
    getCities().forEach((c) => {
      counts[c.id] = c.incidents?.length || 0;
    });
    return counts;
  }, []);

  // Filtered incidents for the right sidebar
  const filteredIncidents = useMemo(() => {
    if (activeFilter === 'ALL') return currentIncidents;
    if (activeFilter === 'CRITICAL') return currentIncidents.filter((i) => i.severity === 'CRITICAL');
    return currentIncidents.filter((i) => i.reportedStatus === activeFilter);
  }, [currentIncidents, activeFilter]);

  // Situation metrics for this city
  const blockedCount = currentIncidents.filter((r) => r.reportedStatus === 'BLOCKED').length;
  const floodedCount = currentIncidents.filter((r) => r.reportedStatus === 'FLOODED').length;
  const flaggedCount = currentIncidents.filter((r) => r.flaggedForReview).length;

  const depots      = currentLocations.filter((l) => l.type === 'DEPOT');
  const reliefCamps = currentLocations.filter((l) => l.type === 'RELIEF_CAMP');
  const hospitals   = currentLocations.filter((l) => l.type === 'HOSPITAL');

  // ── Handlers ───────────────────────────────────────────────────────────────
  const handleOriginChange = (e) => {
    const loc = currentLocations.find((l) => l.id === parseInt(e.target.value));
    setOrigin(loc || null);
  };

  const handleDestinationChange = (e) => {
    const loc = currentLocations.find((l) => l.id === parseInt(e.target.value));
    setDestination(loc || null);
  };

  const handleTriggerTinyFish = async () => {
    setSyncing(true);
    setSyncMsg('');
    try {
      const resp = await triggerTinyFishSync();
      setSyncMsg(`✓ Ingested ${resp?.extractedCount || 0} reports from TinyFish!`);
      await fetchBackend();
      setTimeout(() => setSyncMsg(''), 5000);
    } catch (err) {
      setSyncMsg('⚠ Sync failed: ' + (err.message || 'Service unavailable'));
      setTimeout(() => setSyncMsg(''), 5000);
    } finally {
      setSyncing(false);
    }
  };

  const handleReviewReport = async (reportId, reviewData) => {
    try {
      await submitReportReview(reportId, reviewData);
      await fetchBackend();
    } catch (err) {
      console.warn('Review submission offline:', err.message);
    }
  };

  const handleCenterOnTeam = () => {
    if (liveLocation.coords) {
      setSelectedIncident({
        coordinates: { lat: liveLocation.coords.lat, lng: liveLocation.coords.lng },
      });
    }
  };

  return (
    <div className="app-shell">
      {/* ═══ TOP HEADER ═════════════════════════════════════════════════════ */}
      <Header
        systemStatus={systemStatus}
        backendError={backendError}
        selectedCityId={selectedCityId}
        onSelectCity={setSelectedCityId}
        cityIncidentCounts={cityIncidentCounts}
      />

      <div className="dashboard">
        {/* ═══ LEFT PANEL (Dispatcher Command & Controls) ═══════════════════ */}
        <aside className="panel-left">
          {/* Live Rescue Team GPS Tracking Control */}
          <div className="panel-section">
            <LiveLocationControl
              isTracking={liveLocation.isTracking}
              status={liveLocation.status}
              coords={liveLocation.coords}
              error={liveLocation.error}
              startTracking={liveLocation.startTracking}
              stopTracking={liveLocation.stopTracking}
              onCenterOnTeam={handleCenterOnTeam}
            />
          </div>

          {/* Route Planning (Depot & Camp Selection) */}
          <div className="panel-section">
            <div className="section-header">
              <span className="section-title">🏭 Route Planning</span>
              <span className="section-count">{selectedCity.name}</span>
            </div>
            <div className="section-body">
              <div className="form-group">
                <label className="form-label" htmlFor="select-origin">Origin Depot</label>
                <select
                  id="select-origin"
                  className="form-select"
                  value={origin?.id || ''}
                  onChange={handleOriginChange}
                >
                  <option value="" disabled>Select origin depot…</option>
                  {depots.map((d) => (
                    <option key={d.id} value={d.id}>{d.name}</option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="select-destination">Destination Relief Camp</label>
                <select
                  id="select-destination"
                  className="form-select"
                  value={destination?.id || ''}
                  onChange={handleDestinationChange}
                >
                  <option value="" disabled>Select destination relief camp…</option>
                  {reliefCamps.map((c) => (
                    <option key={c.id} value={c.id}>{c.name}</option>
                  ))}
                </select>
              </div>

              <button
                id="btn-calculate-route"
                className="btn btn-primary btn-full"
                disabled={!origin || !destination}
                title={!origin || !destination ? 'Select both depot and relief camp first' : 'Calculate route'}
              >
                🗺️ Calculate Route
              </button>

              {!origin && !destination && (
                <div style={{ fontSize: 11, color: 'var(--clr-text-muted)', marginTop: 8, fontStyle: 'italic' }}>
                  Select depot & camp in {selectedCity.name} to plan relief routing
                </div>
              )}
            </div>
          </div>

          {/* Situation Metric Cards */}
          <div className="panel-section">
            <div className="section-header">
              <span className="section-title">📊 Sector Situation</span>
              <span style={{ fontSize: 10, color: 'var(--clr-text-muted)' }}>{selectedCity.name}</span>
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

          {/* Route Panel Placeholder (Phase 2) */}
          <RoutePanel origin={origin} destination={destination} />

          {/* Route and Marker Legend */}
          <RouteLegend />

          {/* Hospitals in this City */}
          {hospitals.length > 0 && (
            <div className="panel-section panel-section-scrollable">
              <div className="section-header">
                <span className="section-title">🏥 Hospitals ({selectedCity.name})</span>
                <span className="section-count">{hospitals.length}</span>
              </div>
              <div className="section-body">
                {hospitals.map((h) => (
                  <div
                    key={h.id}
                    style={{
                      marginBottom: 8,
                      fontSize: 12,
                      padding: '6px 8px',
                      background: 'var(--clr-surface-2)',
                      borderRadius: 'var(--radius-sm)',
                      border: '1px solid var(--clr-border)',
                    }}
                  >
                    <div style={{ fontWeight: 600, color: '#f1f5f9' }}>{h.name}</div>
                    <div style={{ color: 'var(--clr-text-muted)', fontSize: 11, marginTop: 2 }}>{h.address}</div>
                    {h.notes && (
                      <div style={{ color: '#38bdf8', fontSize: 10, marginTop: 3 }}>{h.notes}</div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}
        </aside>

        {/* ═══ CENTER INTERACTIVE MAP ═══════════════════════════════════════ */}
        <main className="map-container">
          <ResQMap
            city={selectedCity}
            incidents={currentIncidents}
            hazardZones={hazardZones}
            locations={currentLocations}
            liveCoords={liveLocation.coords}
            selectedIncident={selectedIncident}
            onSelectIncident={setSelectedIncident}
          />

          {/* Sector Overlay Badge */}
          <div
            className="map-overlay-badge"
            style={{
              position: 'absolute',
              bottom: 14,
              right: 14,
              background: 'rgba(11, 15, 26, 0.88)',
              backdropFilter: 'blur(8px)',
              border: '1px solid var(--clr-border)',
              borderRadius: 'var(--radius-sm)',
              padding: '6px 12px',
              fontSize: 11,
              color: 'var(--clr-text)',
              display: 'flex',
              alignItems: 'center',
              gap: 8,
              zIndex: 10,
            }}
          >
            <span>📍 <strong>Sector:</strong> {selectedCity.name}</span>
            <span style={{ color: 'var(--clr-text-muted)' }}>•</span>
            <span style={{ color: 'var(--clr-accent)' }}>{selectedCity.region}</span>
            <span style={{ color: 'var(--clr-text-muted)' }}>•</span>
            <span style={{ color: '#f59e0b', fontSize: 10, fontWeight: 700 }}>DEMO DATA</span>
          </div>
        </main>

        {/* ═══ RIGHT PANEL (City Incidents & Reports) ═══════════════════════ */}
        <aside className="panel-right">
          <div className="section-header panel-section" style={{ display: 'flex', flexDirection: 'column', gap: 8, alignItems: 'stretch' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div>
                <span className="section-title">📋 {selectedCity.name} Reports</span>
                <div style={{ fontSize: 10, color: 'var(--clr-text-muted)', marginTop: 2 }}>
                  All items marked [DEMO] for simulation
                </div>
              </div>
              <span className="section-count">{currentIncidents.length}</span>
            </div>

            {/* Severity Filter Tabs */}
            <div style={{ display: 'flex', gap: 4, background: 'var(--clr-surface-2)', padding: 3, borderRadius: 'var(--radius-sm)' }}>
              {[
                { id: 'ALL', label: 'All' },
                { id: 'CRITICAL', label: 'Critical' },
                { id: 'BLOCKED', label: 'Blocked' },
                { id: 'FLOODED', label: 'Flooded' },
              ].map((tab) => (
                <button
                  key={tab.id}
                  onClick={() => setActiveFilter(tab.id)}
                  style={{
                    flex: 1,
                    background: activeFilter === tab.id ? 'var(--clr-primary)' : 'transparent',
                    color: activeFilter === tab.id ? '#ffffff' : 'var(--clr-text-dim)',
                    border: 'none',
                    borderRadius: 3,
                    fontSize: 10,
                    fontWeight: 600,
                    padding: '3px 0',
                    cursor: 'pointer',
                    transition: 'var(--transition-fast)',
                  }}
                >
                  {tab.label}
                </button>
              ))}
            </div>

            {/* TinyFish Public Reports Trigger */}
            <button
              onClick={handleTriggerTinyFish}
              disabled={syncingTinyFish}
              className="btn btn-secondary"
              style={{
                width: '100%',
                fontSize: 11,
                padding: '5px 8px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: 6,
                background: 'rgba(59,130,246,0.12)',
                borderColor: 'rgba(59,130,246,0.3)',
                color: '#60a5fa',
              }}
            >
              {syncingTinyFish ? (
                <>
                  <div className="loading-spinner" style={{ width: 12, height: 12 }} />
                  Syncing Public Feeds…
                </>
              ) : (
                <>🎣 Sync TinyFish Public Reports</>
              )}
            </button>

            {syncSuccessMsg && (
              <div style={{ fontSize: 11, color: syncSuccessMsg.startsWith('✓') ? '#4ade80' : '#f87171', textAlign: 'center' }}>
                {syncSuccessMsg}
              </div>
            )}
          </div>

          {/* Incident List */}
          {loadingBackend && currentIncidents.length === 0 ? (
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 10, padding: 24, color: 'var(--clr-text-muted)', fontSize: 13 }}>
              <div className="loading-spinner" /> Loading reports…
            </div>
          ) : filteredIncidents.length === 0 ? (
            <div className="empty-state">
              <div className="empty-state-icon">📭</div>
              <div>No road-condition reports match "{activeFilter}".</div>
            </div>
          ) : (
            <div className="panel-section-scrollable" style={{ padding: '8px 12px' }}>
              <div style={{ fontSize: 10, color: 'var(--clr-text-muted)', marginBottom: 8, fontStyle: 'italic' }}>
                💡 Click any incident below to center and highlight it on the map:
              </div>

              {filteredIncidents.map((r) => {
                const isSelected = selectedIncident?.id === r.id;
                return (
                  <div
                    key={r.id}
                    onClick={() => setSelectedIncident(r)}
                    style={{
                      cursor: 'pointer',
                      borderRadius: 'var(--radius-sm)',
                      outline: isSelected ? '2px solid var(--clr-accent)' : 'none',
                      boxShadow: isSelected ? '0 0 12px rgba(6, 182, 212, 0.4)' : 'none',
                      transition: 'var(--transition-fast)',
                      marginBottom: 8,
                    }}
                  >
                    <ReportCard report={r} onReview={handleReviewReport} />
                  </div>
                );
              })}
            </div>
          )}
        </aside>
      </div>
    </div>
  );
}
