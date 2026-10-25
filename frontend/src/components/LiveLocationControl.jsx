import React from 'react';

/**
 * LiveLocationControl — Dispatcher control for rescue-team device GPS tracking.
 *
 * Requirements:
 *  - Explicit user activation via "Share My Location" / "Track Rescue Team"
 *  - Clear Stop Tracking control invoking clearWatch
 *  - Informative status pill (IDLE, REQUESTING, TRACKING, DENIED, ERROR)
 *  - Accuracy indicator and coordinate display
 *  - Graceful instructions on permission denial
 */
export default function LiveLocationControl({
  isTracking,
  status,
  coords,
  error,
  startTracking,
  stopTracking,
  onCenterOnTeam,
}) {
  const isRequesting = status === 'REQUESTING';
  const isDenied = status === 'DENIED';
  const isError = status === 'ERROR';

  return (
    <div
      style={{
        background: 'var(--clr-surface-2)',
        border: '1px solid var(--clr-border)',
        borderRadius: 'var(--radius-sm)',
        padding: '10px 12px',
        display: 'flex',
        flexDirection: 'column',
        gap: 8,
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <span style={{ fontSize: 13 }}>🛰️</span>
          <span style={{ fontSize: 11, fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.5px' }}>
            Rescue Team GPS
          </span>
        </div>

        {/* Status Pill */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          {isTracking && (
            <span
              style={{
                width: 8,
                height: 8,
                borderRadius: '50%',
                background: '#06b6d4',
                boxShadow: '0 0 8px #06b6d4',
                animation: 'pulse 1.5s infinite',
              }}
            />
          )}
          <span
            style={{
              fontSize: 10,
              fontWeight: 700,
              padding: '2px 6px',
              borderRadius: 10,
              background:
                isTracking ? 'rgba(6, 182, 212, 0.15)' :
                isDenied ? 'rgba(239, 68, 68, 0.15)' :
                isRequesting ? 'rgba(245, 158, 11, 0.15)' : 'var(--clr-surface-3)',
              color:
                isTracking ? '#38bdf8' :
                isDenied ? '#fca5a5' :
                isRequesting ? '#fbbf24' : 'var(--clr-text-muted)',
              textTransform: 'uppercase',
            }}
          >
            {isTracking ? 'LIVE TRACKING' : isRequesting ? 'ACQUIRING GPS' : isDenied ? 'ACCESS DENIED' : 'OFFLINE'}
          </span>
        </div>
      </div>

      {/* Action Buttons */}
      <div style={{ display: 'flex', gap: 6 }}>
        {!isTracking ? (
          <button
            onClick={startTracking}
            disabled={isRequesting}
            className="btn btn-primary"
            style={{
              flex: 1,
              fontSize: 11,
              padding: '6px 10px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: 6,
            }}
            title="Enable browser geolocation to share your current rescue team position on the map"
          >
            {isRequesting ? (
              <>
                <div className="loading-spinner" style={{ width: 12, height: 12 }} />
                Waiting for Permission…
              </>
            ) : (
              <>📍 Track Rescue Team</>
            )}
          </button>
        ) : (
          <>
            <button
              onClick={stopTracking}
              className="btn btn-secondary"
              style={{
                flex: 1,
                fontSize: 11,
                padding: '6px 10px',
                borderColor: 'rgba(239,68,68,0.4)',
                color: '#f87171',
                background: 'rgba(239,68,68,0.08)',
              }}
              title="Stop broadcasting device location and clear watch"
            >
              ⏹️ Stop Tracking
            </button>
            {coords?.lat && (
              <button
                onClick={onCenterOnTeam}
                className="btn btn-secondary"
                style={{
                  fontSize: 11,
                  padding: '6px 10px',
                  borderColor: 'rgba(6, 182, 212, 0.4)',
                  color: '#38bdf8',
                  background: 'rgba(6, 182, 212, 0.08)',
                }}
                title="Pan map to current device coordinates"
              >
                🎯 Focus Me
              </button>
            )}
          </>
        )}
      </div>

      {/* Live Coordinates Readout */}
      {isTracking && coords?.lat && (
        <div
          style={{
            fontSize: 10,
            color: 'var(--clr-text-dim)',
            background: 'var(--clr-surface-3)',
            padding: '6px 8px',
            borderRadius: 'var(--radius-sm)',
            display: 'grid',
            gridTemplateColumns: '1fr 1fr',
            gap: 4,
            fontFamily: 'JetBrains Mono, monospace',
          }}
        >
          <div>LAT: <strong>{coords.lat.toFixed(5)}°</strong></div>
          <div>LNG: <strong>{coords.lng.toFixed(5)}°</strong></div>
          <div>ACCURACY: <strong style={{ color: '#38bdf8' }}>±{coords.accuracy}m</strong></div>
          <div>UPDATED: <strong>{new Date(coords.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })}</strong></div>
        </div>
      )}

      {/* Permission Denied / Error guidance */}
      {error && (
        <div
          style={{
            fontSize: 11,
            color: '#fca5a5',
            background: 'rgba(239,68,68,0.12)',
            border: '1px solid rgba(239,68,68,0.25)',
            borderRadius: 'var(--radius-sm)',
            padding: '6px 8px',
            lineHeight: 1.4,
          }}
        >
          <strong>Notice: </strong> {error}
          {isDenied && (
            <div style={{ marginTop: 4, fontSize: 10, color: '#fed7d7' }}>
              💡 Tip: Click the padlock icon in your browser URL bar and set Location to "Allow".
            </div>
          )}
        </div>
      )}

      <div style={{ fontSize: 9, color: 'var(--clr-text-muted)', textAlign: 'center' }}>
        Explicit consent required • Only tracks while active
      </div>
    </div>
  );
}
