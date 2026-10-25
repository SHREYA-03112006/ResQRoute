import React from 'react';

/**
 * RouteLegend — shows the road-status, disaster-zone, facility, and live team tracking key.
 */
export default function RouteLegend() {
  const roadItems = [
    { color: '#ef4444', label: 'Blocked / Critical Hazard', symbol: '✕' },
    { color: '#f59e0b', label: 'Flooded (High Delay/Risk)', symbol: '~' },
    { color: '#a855f7', label: 'Uncertain (Under Review)', symbol: '!' },
    { color: '#22c55e', label: 'Open / Cleared Corridor', symbol: '✓' },
  ];

  const zoneItems = [
    { color: 'rgba(239,68,68,0.3)', border: '#ef4444', label: 'Disaster Hazard Perimeter [DEMO]' },
  ];

  const facilityItems = [
    { color: '#3b82f6', label: 'Relief Logistics Depot', symbol: '📦' },
    { color: '#10b981', label: 'Evacuation Relief Camp', symbol: '⛺' },
    { color: '#f97316', label: 'Emergency Trauma Hospital', symbol: '🏥' },
  ];

  const liveTeamItem = {
    color: '#06b6d4',
    label: 'Live Rescue Team (Device GPS)',
  };

  return (
    <div className="panel-section">
      <div className="section-header">
        <span className="section-title">🗺️ Map Marker Legend</span>
      </div>
      <div className="legend" style={{ display: 'flex', flexDirection: 'column', gap: 6, padding: '0 16px 12px' }}>
        {/* Road & Incident statuses */}
        <div style={{ fontSize: 10, fontWeight: 700, color: 'var(--clr-text-muted)', textTransform: 'uppercase', marginTop: 2 }}>
          Road Conditions
        </div>
        {roadItems.map((item) => (
          <div key={item.label} className="legend-item" style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 11 }}>
            <div
              style={{
                width: 14,
                height: 14,
                borderRadius: '50%',
                background: item.color,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#fff',
                fontSize: 9,
                fontWeight: 700,
                flexShrink: 0,
              }}
            >
              {item.symbol}
            </div>
            <span>{item.label}</span>
          </div>
        ))}

        {/* Hazard Zone */}
        <div style={{ fontSize: 10, fontWeight: 700, color: 'var(--clr-text-muted)', textTransform: 'uppercase', marginTop: 4 }}>
          Hazard Boundaries
        </div>
        {zoneItems.map((item) => (
          <div key={item.label} className="legend-item" style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 11 }}>
            <div
              style={{
                width: 16,
                height: 12,
                borderRadius: 3,
                background: item.color,
                border: `1.5px dashed ${item.border}`,
                flexShrink: 0,
              }}
            />
            <span>{item.label}</span>
          </div>
        ))}

        {/* Facilities */}
        <div style={{ fontSize: 10, fontWeight: 700, color: 'var(--clr-text-muted)', textTransform: 'uppercase', marginTop: 4 }}>
          Emergency Resources
        </div>
        {facilityItems.map((item) => (
          <div key={item.label} className="legend-item" style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 11 }}>
            <span style={{ fontSize: 12 }}>{item.symbol}</span>
            <span>{item.label}</span>
          </div>
        ))}

        {/* Live Team */}
        <div style={{ fontSize: 10, fontWeight: 700, color: 'var(--clr-text-muted)', textTransform: 'uppercase', marginTop: 4 }}>
          Rescue Units
        </div>
        <div className="legend-item" style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 11 }}>
          <div
            style={{
              width: 12,
              height: 12,
              borderRadius: '50%',
              background: liveTeamItem.color,
              boxShadow: `0 0 8px ${liveTeamItem.color}`,
              flexShrink: 0,
            }}
          />
          <span style={{ color: '#38bdf8', fontWeight: 600 }}>{liveTeamItem.label}</span>
        </div>
      </div>
    </div>
  );
}
