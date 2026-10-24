import React from 'react';

/**
 * RouteLegend — shows the road-status colour key and location type key on the map.
 */
export default function RouteLegend() {
  const roadItems = [
    { color: '#22c55e', label: 'Open road' },
    { color: '#f59e0b', label: 'Flooded (high penalty)' },
    { color: '#ef4444', label: 'Blocked (excluded)' },
    { color: '#a78bfa', label: 'Uncertain (review)' },
  ];

  const locationItems = [
    { color: '#3b82f6', label: 'Relief depot' },
    { color: '#22c55e', label: 'Relief camp' },
    { color: '#f97316', label: 'Hospital' },
  ];

  return (
    <div className="panel-section">
      <div className="section-header">
        <span className="section-title">Legend</span>
      </div>
      <div className="legend">
        {roadItems.map((item) => (
          <div key={item.label} className="legend-item">
            <div className="legend-line" style={{ background: item.color }} />
            <span>{item.label}</span>
          </div>
        ))}
        <div style={{ height: 4 }} />
        {locationItems.map((item) => (
          <div key={item.label} className="legend-item">
            <div
              style={{
                width: 12, height: 12, borderRadius: '50%',
                background: item.color,
                boxShadow: `0 0 6px ${item.color}`,
                flexShrink: 0,
              }}
            />
            <span>{item.label}</span>
          </div>
        ))}
      </div>
    </div>
  );
}
