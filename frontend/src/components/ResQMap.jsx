import React, { useState, useEffect } from 'react';
import { useGoogleMaps } from '../hooks/useGoogleMaps';
import GoogleResQMap from './GoogleResQMap';
import FallbackResQMap from './FallbackResQMap';

/**
 * ResQMap — Unified map component for ResQRoute.
 *
 * Requirements satisfied:
 *  - Primary engine: Google Maps JavaScript API with styled dark tactical theme and road network.
 *  - Reads API key safely from VITE_GOOGLE_MAPS_API_KEY.
 *  - If key is missing or invalid: displays a helpful configuration banner and renders
 *    the interactive FallbackResQMap (Leaflet) with full city zones and live tracking,
 *    never crashing.
 *  - Provides a manual engine toggle so dispatchers/judges can switch between Google Maps and Fallback.
 */
export default function ResQMap({
  city,
  incidents = [],
  hazardZones = [],
  locations = [],
  liveCoords = null,
  selectedIncident = null,
  onSelectIncident,
}) {
  const { isLoaded, loadError, hasKey } = useGoogleMaps();

  // Allow user to toggle between Google Maps and Fallback engine
  const [preferredEngine, setPreferredEngine] = useState('auto'); // 'auto' | 'google' | 'fallback'

  // Decide which engine to actually render
  const useGoogle =
    preferredEngine === 'google' ||
    (preferredEngine === 'auto' && hasKey && isLoaded && !loadError);

  const showConfigWarning = !hasKey || loadError;

  return (
    <div style={{ position: 'relative', width: '100%', height: '100%', overflow: 'hidden' }}>
      {/* ── Top Alert Banner for Google Maps Configuration & Engine Selector ── */}
      <div
        style={{
          position: 'absolute',
          top: 10,
          left: 14,
          zIndex: 1000,
          display: 'flex',
          flexDirection: 'column',
          gap: 6,
          maxWidth: 'calc(100% - 180px)',
        }}
      >
        {/* Missing / Unverified API Key Notice */}
        {showConfigWarning && (
          <div
            style={{
              background: 'rgba(15, 23, 42, 0.94)',
              backdropFilter: 'blur(10px)',
              border: '1px solid rgba(245, 158, 11, 0.4)',
              borderRadius: 'var(--radius-sm)',
              padding: '6px 12px',
              fontSize: 11,
              color: '#fef3c7',
              boxShadow: 'var(--shadow-md)',
              display: 'flex',
              alignItems: 'center',
              gap: 8,
              lineHeight: 1.4,
            }}
          >
            <span style={{ fontSize: 14, flexShrink: 0 }}>ℹ️</span>
            <div>
              <strong>Google Maps API Key {loadError ? 'Error' : 'Not Set'}: </strong>
              {loadError ? (
                <span>{loadError}. Operating via interactive Fallback Map.</span>
              ) : (
                <span>
                  Add <code style={{ color: '#38bdf8', background: 'rgba(56, 189, 248, 0.1)', padding: '1px 4px', borderRadius: 3 }}>VITE_GOOGLE_MAPS_API_KEY</code> to your <code style={{ color: '#38bdf8' }}>.env</code> file. ResQRoute is currently operating using the interactive Fallback Map.
                </span>
              )}
            </div>
          </div>
        )}

        {/* Engine Switcher Pill (if key is configured or user wants to test) */}
        {hasKey && (
          <div
            style={{
              background: 'rgba(15, 23, 42, 0.90)',
              backdropFilter: 'blur(8px)',
              border: '1px solid var(--clr-border)',
              borderRadius: 'var(--radius-sm)',
              padding: '3px 6px',
              display: 'inline-flex',
              alignItems: 'center',
              gap: 6,
              alignSelf: 'flex-start',
              fontSize: 11,
              boxShadow: 'var(--shadow-sm)',
            }}
          >
            <span style={{ color: 'var(--clr-text-muted)', fontSize: 10, textTransform: 'uppercase', fontWeight: 700 }}>
              Engine:
            </span>
            <button
              onClick={() => setPreferredEngine('google')}
              disabled={!isLoaded || Boolean(loadError)}
              style={{
                background: useGoogle ? 'var(--clr-primary)' : 'transparent',
                color: useGoogle ? '#fff' : 'var(--clr-text-dim)',
                border: 'none',
                padding: '2px 7px',
                borderRadius: 4,
                fontSize: 10,
                fontWeight: 600,
                cursor: !isLoaded || loadError ? 'not-allowed' : 'pointer',
                opacity: !isLoaded || loadError ? 0.5 : 1,
              }}
              title="Switch to Google Maps"
            >
              Google Maps
            </button>
            <button
              onClick={() => setPreferredEngine('fallback')}
              style={{
                background: !useGoogle ? 'var(--clr-surface-3)' : 'transparent',
                color: !useGoogle ? '#fff' : 'var(--clr-text-dim)',
                border: 'none',
                padding: '2px 7px',
                borderRadius: 4,
                fontSize: 10,
                fontWeight: 600,
                cursor: 'pointer',
              }}
              title="Switch to Fallback Map (Leaflet/OSM)"
            >
              Fallback (OSM)
            </button>
          </div>
        )}
      </div>

      {/* ── Active Map Engine ── */}
      {useGoogle ? (
        <GoogleResQMap
          city={city}
          incidents={incidents}
          hazardZones={hazardZones}
          locations={locations}
          liveCoords={liveCoords}
          selectedIncident={selectedIncident}
          onSelectIncident={onSelectIncident}
        />
      ) : (
        <FallbackResQMap
          city={city}
          incidents={incidents}
          hazardZones={hazardZones}
          locations={locations}
          liveCoords={liveCoords}
          selectedIncident={selectedIncident}
          onSelectIncident={onSelectIncident}
        />
      )}
    </div>
  );
}
