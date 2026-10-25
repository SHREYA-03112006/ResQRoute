import React, { useEffect } from 'react';
import { MapContainer, TileLayer, Marker, Popup, Circle, LayerGroup, useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

// Fix Leaflet default icon paths
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  iconUrl:       'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  shadowUrl:     'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
});

// Custom circle marker icons
const makeCircleIcon = (color, symbol = '', size = 26) =>
  L.divIcon({
    className: '',
    html: `
      <div style="
        width: ${size}px;
        height: ${size}px;
        background: ${color};
        border: 2px solid #ffffff;
        border-radius: 50%;
        box-shadow: 0 0 10px ${color}, 0 2px 6px rgba(0,0,0,0.6);
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 11px;
        font-weight: 700;
        color: #ffffff;
        font-family: system-ui, sans-serif;
      ">${symbol}</div>
    `,
    iconAnchor: [size / 2, size / 2],
    popupAnchor: [0, -size / 2],
  });

const makeLiveTeamIcon = (size = 32) =>
  L.divIcon({
    className: '',
    html: `
      <div style="position: relative; width: ${size}px; height: ${size}px; display: flex; align-items: center; justify-content: center;">
        <div style="
          position: absolute;
          width: 100%;
          height: 100%;
          border-radius: 50%;
          background: rgba(6, 182, 212, 0.35);
          border: 2px solid #06b6d4;
          animation: ping 1.5s cubic-bezier(0, 0, 0.2, 1) infinite;
        "></div>
        <div style="
          width: 14px;
          height: 14px;
          border-radius: 50%;
          background: #06b6d4;
          border: 2px solid #ffffff;
          box-shadow: 0 0 12px #06b6d4;
        "></div>
      </div>
      <style>
        @keyframes ping {
          75%, 100% { transform: scale(1.8); opacity: 0; }
        }
      </style>
    `,
    iconAnchor: [size / 2, size / 2],
    popupAnchor: [0, -size / 2],
  });

function MapViewController({ city, targetCoords }) {
  const map = useMap();

  useEffect(() => {
    if (targetCoords?.lat && targetCoords?.lng) {
      map.flyTo([targetCoords.lat, targetCoords.lng], 15, { duration: 1.2 });
    } else if (city?.center) {
      map.flyTo([city.center.lat, city.center.lng], city.zoom || 13, { duration: 1.2 });
    }
  }, [city, targetCoords, map]);

  return null;
}

/**
 * FallbackResQMap — Leaflet/OSM fallback map when Google Maps API key is not configured.
 * Full disaster zone circles, road incident markers, and live rescue-team tracking.
 */
export default function FallbackResQMap({
  city,
  incidents = [],
  hazardZones = [],
  locations = [],
  liveCoords = null,
  selectedIncident = null,
  onSelectIncident,
}) {
  const center = city?.center ? [city.center.lat, city.center.lng] : [30.3165, 78.0322];
  const zoom = city?.zoom || 13;

  const targetCoords = selectedIncident?.coordinates || null;

  return (
    <div style={{ position: 'relative', width: '100%', height: '100%' }}>
      <MapContainer
        center={center}
        zoom={zoom}
        style={{ width: '100%', height: '100%' }}
        zoomControl={true}
      >
        {/* Dark CartoDB / OSM tiles */}
        <TileLayer
          attribution='&copy; <a href="https://carto.com/">CARTO</a> &copy; OpenStreetMap'
          url="https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png"
        />

        <MapViewController city={city} targetCoords={targetCoords} />

        {/* ── Hazard Zones ── */}
        <LayerGroup>
          {hazardZones.map((zone) => {
            if (!zone.center?.lat || !zone.center?.lng) return null;
            const isCritical = zone.severity === 'CRITICAL';
            const color = isCritical ? '#ef4444' : zone.severity === 'HIGH' ? '#f59e0b' : '#a855f7';

            return (
              <Circle
                key={zone.id}
                center={[zone.center.lat, zone.center.lng]}
                radius={zone.radiusMeters || 500}
                pathOptions={{
                  color,
                  fillColor: color,
                  fillOpacity: 0.22,
                  weight: 2,
                }}
              >
                <Popup>
                  <div style={{ fontSize: 12 }}>
                    <div style={{ fontSize: 10, fontWeight: 700, color, textTransform: 'uppercase' }}>
                      ⚠ Hazard Perimeter [DEMO]
                    </div>
                    <strong style={{ fontSize: 13 }}>{zone.title}</strong>
                    <div style={{ marginTop: 4, color: '#475569' }}>{zone.description}</div>
                    <div style={{ fontSize: 10, color: '#94a3b8', marginTop: 4 }}>
                      Radius: ~{zone.radiusMeters}m • Severity: {zone.severity}
                    </div>
                  </div>
                </Popup>
              </Circle>
            );
          })}
        </LayerGroup>

        {/* ── Incidents (Phase 2 Disaster Intelligence) ── */}
        <LayerGroup>
          {incidents.map((inc) => {
            const lat = inc.latitude || inc.coordinates?.lat;
            const lng = inc.longitude || inc.coordinates?.lng;
            if (!lat || !lng) return null;

            const categoryKey = inc.disasterCategory || (inc.type ? inc.type.replace('FLASH_', '').replace('WATERLOGGING', 'FLOOD') : 'OTHER');
            const catSymbols = {
              FIRE: '🔥',
              FLOOD: '🌊',
              LANDSLIDE: '⛰️',
              ROAD_BLOCKAGE: '🚫',
              BUILDING_COLLAPSE: '🏚️',
              OTHER: '⚠️',
            };
            const symbol = catSymbols[categoryKey] || '⚠️';

            const isBlocked = inc.reportedStatus === 'BLOCKED';
            const isFlooded = inc.reportedStatus === 'FLOODED';
            const isOpen = inc.reportedStatus === 'OPEN';
            const color = isBlocked ? '#ef4444' : isFlooded ? '#f59e0b' : isOpen ? '#22c55e' : '#a855f7';
            const isLive = inc.dataMode === 'LIVE';

            const icon = makeCircleIcon(color, symbol, 28);

            return (
              <Marker
                key={inc.id}
                position={[lat, lng]}
                icon={icon}
                eventHandlers={{
                  click: () => {
                    if (typeof onSelectIncident === 'function') onSelectIncident(inc);
                  },
                }}
              >
                <Popup>
                  <div style={{ fontSize: 12, lineHeight: 1.4 }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <span style={{
                        fontSize: 9,
                        fontWeight: 800,
                        background: isLive ? '#dcfce7' : '#fef9c3',
                        color: isLive ? '#15803d' : '#854d0e',
                        padding: '1px 5px',
                        borderRadius: 3
                      }}>
                        {isLive ? '🟢 LIVE TINYFISH' : '🟡 DEMO / SAMPLE'}
                      </span>
                      <strong style={{ color }}>{inc.reportedStatus}</strong>
                    </div>
                    <div style={{ fontWeight: 700, fontSize: 13, marginTop: 4 }}>
                      {inc.incidentTitle || inc.title || inc.reportedRoadName}
                    </div>
                    <div style={{ fontSize: 11, color: '#3b82f6', marginTop: 2 }}>
                      🛣️ {inc.reportedRoadName}
                    </div>
                    {inc.locationDescription && (
                      <div style={{ fontSize: 11, marginTop: 4, color: '#334155' }}>
                        {inc.locationDescription}
                      </div>
                    )}
                    <div style={{ fontSize: 10, color: '#94a3b8', marginTop: 6, display: 'flex', justifyContent: 'space-between' }}>
                      <span>Category: <strong>{categoryKey}</strong></span>
                      <span>Confidence: {Math.round((inc.confidenceScore || 0) * 100)}%</span>
                    </div>
                    {inc.verificationStatus && (
                      <div style={{ fontSize: 9, fontWeight: 700, color: '#64748b', marginTop: 4 }}>
                        Status: {inc.verificationStatus}
                      </div>
                    )}
                  </div>
                </Popup>
              </Marker>
            );
          })}
        </LayerGroup>

        {/* ── Facilities ── */}
        <LayerGroup>
          {locations.map((loc) => {
            if (!loc.latitude || !loc.longitude) return null;
            const color = loc.type === 'DEPOT' ? '#3b82f6' : loc.type === 'RELIEF_CAMP' ? '#10b981' : '#f97316';
            const symbol = loc.type === 'DEPOT' ? '📦' : loc.type === 'RELIEF_CAMP' ? '⛺' : '🏥';
            const icon = makeCircleIcon(color, symbol, 24);

            return (
              <Marker key={loc.id} position={[loc.latitude, loc.longitude]} icon={icon}>
                <Popup>
                  <div style={{ fontSize: 12 }}>
                    <div style={{ fontSize: 10, color, fontWeight: 700 }}>{loc.type.replace('_', ' ')}</div>
                    <strong style={{ fontSize: 13 }}>{loc.name}</strong>
                    {loc.address && <div style={{ fontSize: 11, color: '#475569', marginTop: 3 }}>{loc.address}</div>}
                    {loc.notes && <div style={{ fontSize: 10, color: '#64748b', marginTop: 2, fontStyle: 'italic' }}>{loc.notes}</div>}
                  </div>
                </Popup>
              </Marker>
            );
          })}
        </LayerGroup>

        {/* ── Live Rescue Team Marker & Accuracy Circle ── */}
        {liveCoords?.lat && (
          <LayerGroup>
            {liveCoords.accuracy && (
              <Circle
                center={[liveCoords.lat, liveCoords.lng]}
                radius={liveCoords.accuracy}
                pathOptions={{
                  color: '#06b6d4',
                  fillColor: '#06b6d4',
                  fillOpacity: 0.15,
                  weight: 1.5,
                }}
              />
            )}
            <Marker
              position={[liveCoords.lat, liveCoords.lng]}
              icon={makeLiveTeamIcon(32)}
              zIndexOffset={9999}
            >
              <Popup>
                <div style={{ fontSize: 12 }}>
                  <div style={{ fontSize: 10, fontWeight: 800, color: '#0284c7' }}>🟢 LIVE RESCUE TEAM (YOU)</div>
                  <strong>Device Geolocation</strong>
                  <div style={{ fontSize: 11, marginTop: 4 }}>
                    {liveCoords.lat.toFixed(5)}, {liveCoords.lng.toFixed(5)}
                  </div>
                  <div style={{ fontSize: 10, color: '#64748b', marginTop: 2 }}>
                    Accuracy: ±{liveCoords.accuracy || 'N/A'}m
                    {liveCoords.speed ? ` • ${liveCoords.speed} km/h` : ''}
                  </div>
                </div>
              </Popup>
            </Marker>
          </LayerGroup>
        )}
      </MapContainer>

      {/* Fallback Mode Watermark */}
      <div
        style={{
          position: 'absolute',
          bottom: 18,
          left: 14,
          zIndex: 1000,
          background: 'rgba(11, 15, 26, 0.88)',
          backdropFilter: 'blur(6px)',
          border: '1px solid var(--clr-border)',
          borderRadius: 'var(--radius-sm)',
          padding: '4px 8px',
          fontSize: 10,
          color: 'var(--clr-text-muted)',
          display: 'flex',
          alignItems: 'center',
          gap: 6,
        }}
      >
        <span style={{ color: '#f59e0b' }}>Fallback Map Engine (OSM)</span>
        <span>•</span>
        <span>Full Interactive Mode Active</span>
      </div>
    </div>
  );
}
