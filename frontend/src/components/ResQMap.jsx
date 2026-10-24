import React, { useEffect, useRef } from 'react';
import { MapContainer, TileLayer, Marker, Popup, LayerGroup, useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import StatusBadge from './StatusBadge';

// Fix Leaflet default icon paths broken by Vite bundling
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  iconUrl:       'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  shadowUrl:     'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
});

// ─── Custom circle marker icons ──────────────────────────────────────────────
const makeCircleIcon = (color, size = 16) =>
  L.divIcon({
    className: '',
    html: `<div style="
      width:${size}px;height:${size}px;
      background:${color};
      border:2px solid rgba(255,255,255,0.6);
      border-radius:50%;
      box-shadow:0 0 8px ${color};
    "></div>`,
    iconAnchor: [size / 2, size / 2],
    popupAnchor: [0, -size / 2],
  });

const LOCATION_ICONS = {
  DEPOT:       makeCircleIcon('#3b82f6', 18),
  RELIEF_CAMP: makeCircleIcon('#22c55e', 18),
  HOSPITAL:    makeCircleIcon('#f97316', 16),
  DEFAULT:     makeCircleIcon('#94a3b8', 14),
};

const REPORT_ICONS = {
  BLOCKED:   makeCircleIcon('#ef4444', 14),
  FLOODED:   makeCircleIcon('#f59e0b', 14),
  UNCERTAIN: makeCircleIcon('#a78bfa', 12),
  OPEN:      makeCircleIcon('#22c55e', 10),
};

// ─── Component to fly to a selection ─────────────────────────────────────────
function FlyToLocation({ lat, lon, zoom = 12 }) {
  const map = useMap();
  useEffect(() => {
    if (lat && lon) map.flyTo([lat, lon], zoom, { duration: 1.2 });
  }, [lat, lon, zoom, map]);
  return null;
}

/**
 * ResQMap — Leaflet map component for the ResQ-Route dashboard.
 *
 * @param {Object[]} locations  - Array of Location objects from the backend
 * @param {Object[]} reports    - Array of RoadReport objects from the backend
 * @param {Object}   origin     - Selected origin location
 * @param {Object}   destination - Selected destination location
 */
export default function ResQMap({ locations = [], reports = [], origin, destination }) {
  // Centre on Rudraprayag district by default
  const center = [30.40, 79.10];
  const zoom = 10;

  // Derive fly-to target from selected origin
  const flyTarget = origin
    ? { lat: origin.latitude, lon: origin.longitude }
    : destination
    ? { lat: destination.latitude, lon: destination.longitude }
    : null;

  return (
    <MapContainer
      center={center}
      zoom={zoom}
      style={{ width: '100%', height: '100%' }}
      zoomControl={true}
    >
      {/* OpenStreetMap tile layer */}
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />

      {/* Smooth fly-to on location selection */}
      {flyTarget && <FlyToLocation lat={flyTarget.lat} lon={flyTarget.lon} />}

      {/* ── Location markers ── */}
      <LayerGroup>
        {locations.map((loc) => {
          const icon = LOCATION_ICONS[loc.type] || LOCATION_ICONS.DEFAULT;
          return (
            <Marker key={loc.id} position={[loc.latitude, loc.longitude]} icon={icon}>
              <Popup>
                <div>
                  <strong style={{ fontSize: 13 }}>{loc.name}</strong>
                  <div style={{ fontSize: 11, color: '#94a3b8', marginTop: 3 }}>
                    {loc.type.replace('_', ' ')}
                  </div>
                  {loc.address && (
                    <div style={{ fontSize: 11, marginTop: 4 }}>{loc.address}</div>
                  )}
                  {loc.notes && (
                    <div style={{ fontSize: 11, color: '#94a3b8', marginTop: 2, fontStyle: 'italic' }}>
                      {loc.notes}
                    </div>
                  )}
                </div>
              </Popup>
            </Marker>
          );
        })}
      </LayerGroup>

      {/* ── Road report markers ── */}
      <LayerGroup>
        {reports.map((rep) => {
          if (!rep.latitude || !rep.longitude) return null;
          const icon = REPORT_ICONS[rep.reportedStatus] || REPORT_ICONS.UNCERTAIN;
          return (
            <Marker key={rep.id} position={[rep.latitude, rep.longitude]} icon={icon}>
              <Popup>
                <div>
                  <strong style={{ fontSize: 13 }}>{rep.reportedRoadName}</strong>
                  <div style={{ marginTop: 4 }}>
                    Status: <strong>{rep.reportedStatus}</strong>
                  </div>
                  {rep.locationDescription && (
                    <div style={{ fontSize: 11, marginTop: 4 }}>{rep.locationDescription}</div>
                  )}
                  <div style={{ fontSize: 10, color: '#94a3b8', marginTop: 6 }}>
                    {rep.dataMode === 'SAMPLE' ? '⚠ SAMPLE DATA — not verified' : '🟢 Live report'}
                  </div>
                  <div style={{ fontSize: 10, color: '#94a3b8' }}>
                    Confidence: {Math.round((rep.confidenceScore || 0) * 100)}%
                  </div>
                </div>
              </Popup>
            </Marker>
          );
        })}
      </LayerGroup>
    </MapContainer>
  );
}
