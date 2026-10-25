import React from 'react';
import { CITIES } from '../data/cityData';

/**
 * CitySelector — dropdown selector allowing dispatchers to monitor different cities/districts.
 *
 * @param {string} selectedCityId - Currently selected city id
 * @param {Function} onSelectCity - Callback with selected city id
 * @param {Object} cityIncidentCounts - Map of cityId -> incident count
 */
export default function CitySelector({
  selectedCityId,
  onSelectCity,
  cityIncidentCounts = {},
}) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
      <label
        htmlFor="resq-city-select"
        style={{
          fontSize: 11,
          fontWeight: 700,
          color: 'var(--clr-text-muted)',
          textTransform: 'uppercase',
          letterSpacing: '0.5px',
          display: 'flex',
          alignItems: 'center',
          gap: 4,
          whiteSpace: 'nowrap',
        }}
      >
        📍 Sector:
      </label>

      <div style={{ position: 'relative' }}>
        <select
          id="resq-city-select"
          value={selectedCityId}
          onChange={(e) => onSelectCity(e.target.value)}
          className="form-select"
          style={{
            fontSize: 12,
            fontWeight: 600,
            padding: '5px 28px 5px 10px',
            background: 'var(--clr-surface-2)',
            color: 'var(--clr-text)',
            borderColor: 'var(--clr-border-glow)',
            borderRadius: 'var(--radius-sm)',
            cursor: 'pointer',
            minWidth: 160,
          }}
        >
          {CITIES.map((c) => {
            const count = cityIncidentCounts[c.id] || c.incidents?.length || 0;
            return (
              <option key={c.id} value={c.id}>
                {c.name} ({count} active {count === 1 ? 'incident' : 'incidents'})
              </option>
            );
          })}
        </select>
      </div>
    </div>
  );
}
