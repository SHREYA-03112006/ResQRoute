import React, { useEffect, useMemo } from 'react';
import { MapContainer, TileLayer, Marker, Popup, Polyline, LayerGroup, Tooltip, useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

// Fix Leaflet default icon paths broken by Vite bundling
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  iconUrl:       'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  shadowUrl:     'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
});

// ─── Custom circle marker icons ──────────────────────────────────────────────
const makeCircleIcon = (color, size = 16, pulse = false) =>
  L.divIcon({
    className: '',
    html: `<div style="
      width:${size}px;height:${size}px;
      background:${color};
      border:2.5px solid #ffffff;
      border-radius:50%;
      box-shadow:0 0 10px ${color}, 0 0 16px ${color}88;
      ${pulse ? 'animation: pulse-ring 2s infinite;' : ''}
    "></div>`,
    iconAnchor: [size / 2, size / 2],
    popupAnchor: [0, -size / 2],
  });

const LOCATION_ICONS = {
  DEPOT:       makeCircleIcon('#3b82f6', 20),
  RELIEF_CAMP: makeCircleIcon('#22c55e', 20),
  HOSPITAL:    makeCircleIcon('#f97316', 16),
  DEFAULT:     makeCircleIcon('#94a3b8', 14),
};

const REPORT_ICONS = {
  BLOCKED:   makeCircleIcon('#ef4444', 16, true),
  FLOODED:   makeCircleIcon('#f59e0b', 16, true),
  UNCERTAIN: makeCircleIcon('#a78bfa', 14),
  OPEN:      makeCircleIcon('#22c55e', 12),
};

// ─── Component to auto-fit bounds on route change ─────────────────────────────
function AutoFitBounds({ waypoints }) {
  const map = useMap();
  useEffect(() => {
    if (waypoints && waypoints.length > 1) {
      const bounds = L.latLngBounds(waypoints.map((pt) => [pt[0], pt[1]]));
      map.fitBounds(bounds, { padding: [50, 50], maxZoom: 13, duration: 1.2 });
    }
  }, [waypoints, map]);
  return null;
}

// ─── Component to fly to a selected single location ──────────────────────────
function FlyToLocation({ lat, lon, zoom = 12 }) {
  const map = useMap();
  useEffect(() => {
    if (lat && lon) map.flyTo([lat, lon], zoom, { duration: 1.2 });
  }, [lat, lon, zoom, map]);
  return null;
}

/**
 * ResQMap — Interactive Leaflet map with OSM road graph overlay,
 * primary & alternative calculated route rendering, and disaster hazard markers.
 */
export default function ResQMap({
  locations = [],
  reports = [],
  origin,
  destination,
  roadNetwork = null,
  routeData = null,
  selectedRouteIndex = 0,
  onSelectRouteIndex,
}) {
  // Centre on Chamoli / Rudraprayag district
  const center = [30.40, 79.20];
  const zoom = 10;

  // Extract all routes for polyline rendering
  const allRoutes = useMemo(() => {
    const list = [];
    if (routeData?.primaryRoute) {
      list.push({ ...routeData.primaryRoute, label: 'Primary Route (Optimal)', index: 0 });
    }
    if (routeData?.alternativeRoutes) {
      routeData.alternativeRoutes.forEach((alt, idx) => {
        list.push({ ...alt, label: `Alternative ${idx + 1}`, index: idx + 1 });
      });
    }
    return list;
  }, [routeData]);

  const activeRoute = allRoutes[selectedRouteIndex] || allRoutes[0];

  // Derive fly-to target when no route is active
  const flyTarget = !routeData && origin
    ? { lat: origin.latitude, lon: origin.longitude }
    : !routeData && destination
    ? { lat: destination.latitude, lon: destination.longitude }
    : null;

  return (
    <MapContainer
      center={center}
      zoom={zoom}
      style={{ width: '100%', height: '100%' }}
      zoomControl={true}
    >
      {/* Base OpenStreetMap Tiles */}
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />

      {/* Auto-fit bounds when route waypoints exist */}
      {activeRoute?.waypoints && <AutoFitBounds waypoints={activeRoute.waypoints} />}

      {/* Smooth fly-to on location selection when not routing */}
      {flyTarget && <FlyToLocation lat={flyTarget.lat} lon={flyTarget.lon} />}

      {/* ── 1. OSM Road Network Graph Layer ── */}
      {roadNetwork?.edges && (
        <LayerGroup>
          {roadNetwork.edges.map((edge) => {
            const positions = [
              [edge.fromLat, edge.fromLon],
              [edge.toLat, edge.toLon],
            ];
            const color =
              edge.status === 'BLOCKED'
                ? '#ef4444'
                : edge.status === 'FLOODED'
                ? '#f59e0b'
                : edge.status === 'UNCERTAIN'
                ? '#a855f7'
                : '#475569';
            const weight = edge.status === 'OPEN' ? 2 : 4;
            const dashArray = edge.status === 'BLOCKED' ? '6, 6' : undefined;

            return (
              <Polyline
                key={`edge-${edge.id}`}
                positions={positions}
                pathOptions={{ color, weight, opacity: 0.65, dashArray }}
              >
                <Tooltip sticky>
                  <div style={{ fontSize: 11 }}>
                    <strong>{edge.roadName}</strong> ({edge.roadType})
                    <div>Distance: {edge.distanceKm} km | Est. Time: {edge.baseTravelTimeMinutes} mins</div>
                    <div>Status: <span style={{ fontWeight: 700, color }}>{edge.status}</span></div>
                  </div>
                </Tooltip>
              </Polyline>
            );
          })}
        </LayerGroup>
      )}

      {/* ── 2. Calculated Alternate Routes (Dashed, Clickable) ── */}
      <LayerGroup>
        {allRoutes.map((route) => {
          const isActive = route.index === selectedRouteIndex;
          if (isActive) return null; // Rendered separately for top z-index

          const positions = route.waypoints.map((pt) => [pt[0], pt[1]]);
          return (
            <Polyline
              key={`alt-route-${route.index}`}
              positions={positions}
              pathOptions={{
                color: '#f59e0b',
                weight: 4,
                opacity: 0.7,
                dashArray: '8, 8',
              }}
              eventHandlers={{
                click: () => onSelectRouteIndex && onSelectRouteIndex(route.index),
              }}
            >
              <Tooltip sticky>
                <div style={{ fontSize: 11 }}>
                  <strong>{route.label}</strong> (Click to select)
                  <div>Distance: {route.totalDistanceKm} km</div>
                  <div>Travel Time: {Math.round(route.estimatedTimeMinutes)} mins</div>
                </div>
              </Tooltip>
            </Polyline>
          );
        })}
      </LayerGroup>

      {/* ── 3. Active Primary / Selected Route (Glowing Vibrant Polyline) ── */}
      {activeRoute?.waypoints && (
        <LayerGroup>
          {/* Subtle outer glow polyline */}
          <Polyline
            positions={activeRoute.waypoints.map((pt) => [pt[0], pt[1]])}
            pathOptions={{
              color: '#0284c7',
              weight: 9,
              opacity: 0.35,
              lineCap: 'round',
              lineJoin: 'round',
            }}
          />
          {/* Main sharp vibrant route polyline */}
          <Polyline
            positions={activeRoute.waypoints.map((pt) => [pt[0], pt[1]])}
            pathOptions={{
              color: '#38bdf8',
              weight: 5,
              opacity: 0.95,
              lineCap: 'round',
              lineJoin: 'round',
            }}
          >
            <Popup>
              <div style={{ minWidth: 160 }}>
                <strong style={{ fontSize: 13, color: '#0284c7' }}>
                  {activeRoute.label}
                </strong>
                <div style={{ marginTop: 4, fontSize: 12 }}>
                  Total Distance: <strong>{activeRoute.totalDistanceKm} km</strong>
                </div>
                <div style={{ fontSize: 12 }}>
                  Travel Time: <strong>{Math.round(activeRoute.estimatedTimeMinutes)} mins</strong>
                </div>
                <div style={{ fontSize: 11, color: '#64748b', marginTop: 4 }}>
                  Algorithm: {activeRoute.algorithm}
                </div>
                {activeRoute.floodedSegments > 0 && (
                  <div style={{ fontSize: 11, color: '#d97706', fontWeight: 600, marginTop: 2 }}>
                    ⚠ Contains {activeRoute.floodedSegments} flooded section(s)
                  </div>
                )}
              </div>
            </Popup>
          </Polyline>
        </LayerGroup>
      )}

      {/* ── 4. Origin & Destination Special Highlight Markers ── */}
      {origin && (
        <Marker
          position={[origin.latitude, origin.longitude]}
          icon={makeCircleIcon('#2563eb', 22, true)}
        >
          <Popup>
            <div>
              <strong>Origin Depot: {origin.name}</strong>
              <div style={{ fontSize: 11, color: '#64748b' }}>{origin.address}</div>
            </div>
          </Popup>
        </Marker>
      )}

      {destination && (
        <Marker
          position={[destination.latitude, destination.longitude]}
          icon={makeCircleIcon('#16a34a', 22, true)}
        >
          <Popup>
            <div>
              <strong>Destination: {destination.name}</strong>
              <div style={{ fontSize: 11, color: '#64748b' }}>{destination.address}</div>
            </div>
          </Popup>
        </Marker>
      )}

      {/* ── 5. Standard Location Markers ── */}
      <LayerGroup>
        {locations.map((loc) => {
          // If already highlighted as origin/dest, skip duplicate marker
          if (origin?.id === loc.id || destination?.id === loc.id) return null;
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

      {/* ── 6. Disaster Road Report Markers ── */}
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
                    Status: <strong style={{
                      color: rep.reportedStatus === 'BLOCKED' ? '#ef4444' :
                             rep.reportedStatus === 'FLOODED' ? '#f59e0b' : '#a855f7'
                    }}>{rep.reportedStatus}</strong>
                  </div>
                  {rep.locationDescription && (
                    <div style={{ fontSize: 11, marginTop: 4 }}>{rep.locationDescription}</div>
                  )}
                  <div style={{ fontSize: 10, color: '#94a3b8', marginTop: 6 }}>
                    {rep.dataMode === 'SAMPLE' ? '⚠ SAMPLE DATA' : '🟢 Verified Live report'}
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
