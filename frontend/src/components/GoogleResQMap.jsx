import React, { useEffect, useRef, useState } from 'react';

/**
 * Dark tactical emergency theme for Google Maps.
 * Matches ResQRoute's midnight cyber command-center aesthetic, highlighting
 * highways, arterial roads, and water bodies with high contrast.
 */
const DARK_TACTICAL_MAP_STYLE = [
  { elementType: 'geometry', stylers: [{ color: '#0b1320' }] },
  { elementType: 'labels.text.stroke', stylers: [{ color: '#0b1320' }] },
  { elementType: 'labels.text.fill', stylers: [{ color: '#94a3b8' }] },
  {
    featureType: 'administrative.locality',
    elementType: 'labels.text.fill',
    stylers: [{ color: '#e2e8f0' }],
  },
  {
    featureType: 'poi',
    elementType: 'labels.text.fill',
    stylers: [{ color: '#64748b' }],
  },
  {
    featureType: 'poi.park',
    elementType: 'geometry',
    stylers: [{ color: '#0f242e' }],
  },
  {
    featureType: 'road',
    elementType: 'geometry',
    stylers: [{ color: '#1e293b' }],
  },
  {
    featureType: 'road',
    elementType: 'geometry.stroke',
    stylers: [{ color: '#0f172a' }],
  },
  {
    featureType: 'road',
    elementType: 'labels.text.fill',
    stylers: [{ color: '#cbd5e1' }],
  },
  {
    featureType: 'road.highway',
    elementType: 'geometry',
    stylers: [{ color: '#334155' }],
  },
  {
    featureType: 'road.highway',
    elementType: 'geometry.stroke',
    stylers: [{ color: '#1e293b' }],
  },
  {
    featureType: 'road.highway',
    elementType: 'labels.text.fill',
    stylers: [{ color: '#38bdf8' }],
  },
  {
    featureType: 'road.arterial',
    elementType: 'geometry',
    stylers: [{ color: '#27354a' }],
  },
  {
    featureType: 'transit',
    elementType: 'geometry',
    stylers: [{ color: '#1a2234' }],
  },
  {
    featureType: 'water',
    elementType: 'geometry',
    stylers: [{ color: '#062038' }],
  },
  {
    featureType: 'water',
    elementType: 'labels.text.fill',
    stylers: [{ color: '#38bdf8' }],
  },
];

/** SVG Pin Generator for Google Maps Marker */
function createMarkerSvgIcon(color, symbol, size = 32) {
  const svg = `
    <svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size + 8}" viewBox="0 0 32 40">
      <defs>
        <filter id="shadow" x="-20%" y="-20%" width="140%" height="140%">
          <feDropShadow dx="0" dy="2" stdDeviation="2" flood-color="rgba(0,0,0,0.6)"/>
        </filter>
      </defs>
      <path d="M16 2 C8.27 2 2 8.27 2 16 C2 26 16 38 16 38 C16 38 30 26 30 16 C30 8.27 23.73 2 16 2 Z"
            fill="${color}" stroke="#ffffff" stroke-width="1.5" filter="url(#shadow)"/>
      <circle cx="16" cy="15" r="9" fill="#0b0f1a" opacity="0.9"/>
      <text x="16" y="19" font-size="11" font-weight="bold" fill="#ffffff" text-anchor="middle" font-family="system-ui, sans-serif">
        ${symbol}
      </text>
    </svg>
  `;
  return 'data:image/svg+xml;charset=UTF-8,' + encodeURIComponent(svg.trim());
}

/** SVG Live Team Locator Pulse Icon */
function createLiveTeamSvgIcon(size = 38) {
  const svg = `
    <svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 40 40">
      <circle cx="20" cy="20" r="16" fill="rgba(6, 182, 212, 0.25)" stroke="#06b6d4" stroke-width="2"/>
      <circle cx="20" cy="20" r="8" fill="#06b6d4" stroke="#ffffff" stroke-width="2"/>
      <circle cx="20" cy="20" r="3" fill="#ffffff"/>
    </svg>
  `;
  return 'data:image/svg+xml;charset=UTF-8,' + encodeURIComponent(svg.trim());
}

/**
 * GoogleResQMap — Google Maps interactive component for ResQRoute.
 *
 * @param {Object} city - Current selected city object
 * @param {Array} incidents - List of road incidents/reports for this city
 * @param {Array} hazardZones - List of disaster hazard zones/perimeters
 * @param {Array} locations - List of depots, camps, and hospitals
 * @param {Object} liveCoords - Live coordinates from useLiveLocation hook { lat, lng, accuracy, ... }
 * @param {Object} selectedIncident - Incident currently focused/selected by user
 * @param {Function} onSelectIncident - Callback when user clicks a marker
 */
export default function GoogleResQMap({
  city,
  incidents = [],
  hazardZones = [],
  locations = [],
  liveCoords = null,
  selectedIncident = null,
  onSelectIncident,
}) {
  const mapRef = useRef(null);
  const mapInstanceRef = useRef(null);
  const infoWindowRef = useRef(null);
  const trafficLayerRef = useRef(null);

  // Overlays tracking refs
  const markersRef = useRef([]);
  const zonesRef = useRef([]);
  const teamMarkerRef = useRef(null);
  const teamAccuracyCircleRef = useRef(null);

  const [showTraffic, setShowTraffic] = useState(false);
  const [mapType, setMapType] = useState('roadmap'); // 'roadmap' | 'hybrid'

  // Initialize Map
  useEffect(() => {
    if (!mapRef.current || !window.google || !window.google.maps) return;

    const initialCenter = city?.center || { lat: 30.3165, lng: 78.0322 };
    const initialZoom = city?.zoom || 13;

    const map = new window.google.maps.Map(mapRef.current, {
      center: initialCenter,
      zoom: initialZoom,
      mapTypeId: mapType,
      styles: mapType === 'roadmap' ? DARK_TACTICAL_MAP_STYLE : null,
      zoomControl: true,
      mapTypeControl: false,
      streetViewControl: false,
      fullscreenControl: true,
      rotateControl: false,
      gestureHandling: 'greedy',
    });

    mapInstanceRef.current = map;
    infoWindowRef.current = new window.google.maps.InfoWindow({
      maxWidth: 320,
    });

    trafficLayerRef.current = new window.google.maps.TrafficLayer();

    return () => {
      // Clean up map listeners
      if (infoWindowRef.current) infoWindowRef.current.close();
      if (trafficLayerRef.current) trafficLayerRef.current.setMap(null);
      mapInstanceRef.current = null;
    };
  }, []); // Run once on mount

  // Handle map type and style changes
  useEffect(() => {
    const map = mapInstanceRef.current;
    if (!map || !window.google) return;

    map.setMapTypeId(mapType);
    if (mapType === 'roadmap') {
      map.setOptions({ styles: DARK_TACTICAL_MAP_STYLE });
    } else {
      map.setOptions({ styles: null });
    }
  }, [mapType]);

  // Handle Traffic Layer Toggle
  useEffect(() => {
    const map = mapInstanceRef.current;
    const trafficLayer = trafficLayerRef.current;
    if (!map || !trafficLayer) return;

    if (showTraffic) {
      trafficLayer.setMap(map);
    } else {
      trafficLayer.setMap(null);
    }
  }, [showTraffic]);

  // Handle City Change: Smooth pan and zoom
  useEffect(() => {
    const map = mapInstanceRef.current;
    if (!map || !city?.center) return;

    map.panTo(city.center);
    map.setZoom(city.zoom || 13);
    if (infoWindowRef.current) infoWindowRef.current.close();
  }, [city?.id, city?.center, city?.zoom]);

  // Render Hazard Zones (Circles/Perimeters)
  useEffect(() => {
    const map = mapInstanceRef.current;
    if (!map || !window.google) return;

    // Clear old zone circles
    zonesRef.current.forEach((z) => z.setMap(null));
    zonesRef.current = [];

    const severityConfig = {
      CRITICAL: { strokeColor: '#ef4444', fillColor: '#ef4444', fillOpacity: 0.24 },
      HIGH:     { strokeColor: '#f59e0b', fillColor: '#f59e0b', fillOpacity: 0.20 },
      MODERATE: { strokeColor: '#a855f7', fillColor: '#a855f7', fillOpacity: 0.18 },
      LOW:      { strokeColor: '#22c55e', fillColor: '#22c55e', fillOpacity: 0.15 },
    };

    hazardZones.forEach((zone) => {
      if (!zone.center?.lat || !zone.center?.lng) return;

      const conf = severityConfig[zone.severity] || severityConfig.MODERATE;
      const circle = new window.google.maps.Circle({
        map,
        center: zone.center,
        radius: zone.radiusMeters || 500,
        strokeColor: conf.strokeColor,
        strokeOpacity: 0.85,
        strokeWeight: 2,
        fillColor: conf.fillColor,
        fillOpacity: conf.fillOpacity,
        clickable: true,
        zIndex: 5,
      });

      circle.addListener('click', (e) => {
        if (!infoWindowRef.current) return;
        const html = `
          <div style="font-family:Inter,system-ui,sans-serif;padding:6px 4px;color:#0f172a;max-width:280px;">
            <div style="font-size:10px;font-weight:700;color:${conf.strokeColor};text-transform:uppercase;letter-spacing:0.5px;">
              ⚠ AFFECTED DISASTER ZONE [DEMO]
            </div>
            <div style="font-size:13px;font-weight:700;margin-top:2px;color:#0f172a;">${zone.title}</div>
            <div style="font-size:11px;color:#475569;margin-top:4px;">${zone.description}</div>
            <div style="margin-top:6px;font-size:10px;background:#f1f5f9;padding:3px 6px;border-radius:4px;color:#64748b;">
              Perimeter Radius: ~${zone.radiusMeters}m • Severity: <strong>${zone.severity}</strong>
            </div>
          </div>
        `;
        infoWindowRef.current.setContent(html);
        infoWindowRef.current.setPosition(e.latLng || zone.center);
        infoWindowRef.current.open(map);
      });

      zonesRef.current.push(circle);
    });

    return () => {
      zonesRef.current.forEach((z) => z.setMap(null));
      zonesRef.current = [];
    };
  }, [hazardZones]);

  // Render Incident & Facility Markers
  useEffect(() => {
    const map = mapInstanceRef.current;
    if (!map || !window.google) return;

    // Clear old markers
    markersRef.current.forEach((m) => m.setMap(null));
    markersRef.current = [];

    // Helper to open incident info
    const openIncidentPopup = (inc, marker) => {
      if (!infoWindowRef.current) return;
      const statusColor =
        inc.reportedStatus === 'BLOCKED' ? '#ef4444' :
        inc.reportedStatus === 'FLOODED' ? '#f59e0b' :
        inc.reportedStatus === 'OPEN' ? '#22c55e' : '#a855f7';

      const isLive = inc.dataMode === 'LIVE';
      const categoryKey = inc.disasterCategory || (inc.type ? inc.type.replace('FLASH_', '').replace('WATERLOGGING', 'FLOOD') : 'OTHER');
      const timeStr = inc.observedAt || inc.timestamp ? new Date(inc.observedAt || inc.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : 'Recent';

      const content = `
        <div style="font-family:Inter,system-ui,sans-serif;padding:8px 4px;color:#0f172a;line-height:1.4;">
          <div style="display:flex;align-items:center;justify-content:space-between;gap:8px;margin-bottom:4px;">
            <span style="font-size:10px;font-weight:800;color:${isLive ? '#15803d' : '#854d0e'};background:${isLive ? '#dcfce7' : '#fef9c3'};padding:2px 6px;border-radius:4px;letter-spacing:0.5px;">
              ${isLive ? '🟢 LIVE TINYFISH' : '🟡 DEMO / SAMPLE'}
            </span>
            <span style="font-size:10px;font-weight:700;color:${statusColor};background:#f8fafc;padding:2px 6px;border-radius:4px;border:1px solid ${statusColor};">
              ${inc.reportedStatus}
            </span>
          </div>
          <div style="font-size:14px;font-weight:700;color:#0f172a;margin-bottom:2px;">
            ${inc.incidentTitle || inc.title || inc.reportedRoadName}
          </div>
          <div style="font-size:11px;font-weight:600;color:#3b82f6;margin-bottom:6px;">
            🛣️ ${inc.reportedRoadName}
          </div>
          ${inc.locationDescription ? `
            <div style="font-size:11px;color:#334155;background:#f8fafc;padding:6px 8px;border-radius:6px;border-left:3px solid ${statusColor};margin-bottom:8px;">
              ${inc.locationDescription}
            </div>
          ` : ''}
          <div style="display:grid;grid-template-columns:1fr 1fr;gap:6px;font-size:10px;color:#64748b;">
            <div>Severity: <strong style="color:#0f172a;">${inc.severity || 'MODERATE'}</strong></div>
            <div>Confidence: <strong style="color:#0f172a;">${Math.round((inc.confidenceScore || 0) * 100)}%</strong></div>
            <div>Category: <strong style="color:#0f172a;">${categoryKey}</strong></div>
            <div>Time: <strong style="color:#0f172a;">${timeStr}</strong></div>
          </div>
          ${inc.verificationStatus ? `
            <div style="margin-top:6px;font-size:10px;font-weight:700;color:#475569;">
              Pipeline Verification: <span style="color:#0f172a;">${inc.verificationStatus}</span>
            </div>
          ` : ''}
          ${inc.sourceName ? `
            <div style="margin-top:6px;font-size:9px;color:#94a3b8;border-top:1px dashed #cbd5e1;padding-top:4px;">
              Source: ${inc.sourceName.replace('[SAMPLE] ', '')}
            </div>
          ` : ''}
        </div>
      `;
      infoWindowRef.current.setContent(content);
      infoWindowRef.current.open(map, marker);
      if (typeof onSelectIncident === 'function') {
        onSelectIncident(inc);
      }
    };

    // 1. Incidents (Phase 2 Disaster Intelligence)
    incidents.forEach((inc) => {
      const lat = inc.latitude || inc.coordinates?.lat;
      const lng = inc.longitude || inc.coordinates?.lng;
      if (!lat || !lng) return;

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

      const marker = new window.google.maps.Marker({
        position: { lat, lng },
        map,
        title: inc.incidentTitle || inc.title || inc.reportedRoadName,
        icon: {
          url: createMarkerSvgIcon(color, symbol, 30),
          scaledSize: new window.google.maps.Size(30, 38),
          anchor: new window.google.maps.Point(15, 38),
        },
        zIndex: 50,
      });

      marker.addListener('click', () => openIncidentPopup(inc, marker));
      marker._incidentId = inc.id;
      markersRef.current.push(marker);
    });

    // 2. Facilities (Depots, Camps, Hospitals)
    locations.forEach((loc) => {
      if (!loc.latitude || !loc.longitude) return;

      const isDepot = loc.type === 'DEPOT';
      const isCamp = loc.type === 'RELIEF_CAMP';
      const isHospital = loc.type === 'HOSPITAL';

      const color = isDepot ? '#3b82f6' : isCamp ? '#10b981' : isHospital ? '#f97316' : '#94a3b8';
      const symbol = isDepot ? '📦' : isCamp ? '⛺' : isHospital ? '🏥' : '📍';

      const marker = new window.google.maps.Marker({
        position: { lat: loc.latitude, lng: loc.longitude },
        map,
        title: loc.name,
        icon: {
          url: createMarkerSvgIcon(color, symbol, 28),
          scaledSize: new window.google.maps.Size(28, 35),
          anchor: new window.google.maps.Point(14, 35),
        },
        zIndex: 20,
      });

      marker.addListener('click', () => {
        if (!infoWindowRef.current) return;
        const content = `
          <div style="font-family:Inter,system-ui,sans-serif;padding:6px 4px;color:#0f172a;line-height:1.4;">
            <div style="font-size:10px;font-weight:700;color:${color};text-transform:uppercase;letter-spacing:0.5px;">
              ${symbol} ${loc.type.replace('_', ' ')}
            </div>
            <div style="font-size:13px;font-weight:700;margin:2px 0;color:#0f172a;">${loc.name}</div>
            <div style="font-size:11px;color:#475569;margin-bottom:4px;">${loc.address || ''}</div>
            ${loc.notes ? `<div style="font-size:11px;color:#64748b;font-style:italic;background:#f8fafc;padding:4px 6px;border-radius:4px;">${loc.notes}</div>` : ''}
          </div>
        `;
        infoWindowRef.current.setContent(content);
        infoWindowRef.current.open(map, marker);
      });

      markersRef.current.push(marker);
    });

    return () => {
      markersRef.current.forEach((m) => m.setMap(null));
      markersRef.current = [];
    };
  }, [incidents, locations, onSelectIncident]);

  // Focus on Selected Incident when updated from list
  useEffect(() => {
    const map = mapInstanceRef.current;
    if (!map || !selectedIncident?.coordinates) return;

    map.panTo(selectedIncident.coordinates);
    map.setZoom(15);

    // Find and trigger marker info window
    const targetMarker = markersRef.current.find((m) => m._incidentId === selectedIncident.id);
    if (targetMarker && window.google) {
      window.google.maps.event.trigger(targetMarker, 'click');
    }
  }, [selectedIncident]);

  // Handle Rescue-Team Live Location
  useEffect(() => {
    const map = mapInstanceRef.current;
    if (!map || !window.google) return;

    // Clean up previous team overlays
    if (teamMarkerRef.current) {
      teamMarkerRef.current.setMap(null);
      teamMarkerRef.current = null;
    }
    if (teamAccuracyCircleRef.current) {
      teamAccuracyCircleRef.current.setMap(null);
      teamAccuracyCircleRef.current = null;
    }

    if (!liveCoords?.lat || !liveCoords?.lng) return;

    const teamPos = { lat: liveCoords.lat, lng: liveCoords.lng };

    // Accuracy Circle
    if (liveCoords.accuracy) {
      teamAccuracyCircleRef.current = new window.google.maps.Circle({
        map,
        center: teamPos,
        radius: liveCoords.accuracy,
        strokeColor: '#06b6d4',
        strokeOpacity: 0.6,
        strokeWeight: 1.5,
        fillColor: '#06b6d4',
        fillOpacity: 0.12,
        clickable: false,
        zIndex: 100,
      });
    }

    // Live Team Marker
    const teamMarker = new window.google.maps.Marker({
      position: teamPos,
      map,
      title: 'Rescue Team Live Location',
      icon: {
        url: createLiveTeamSvgIcon(36),
        scaledSize: new window.google.maps.Size(36, 36),
        anchor: new window.google.maps.Point(18, 18),
      },
      zIndex: 9999,
    });

    teamMarker.addListener('click', () => {
      if (!infoWindowRef.current) return;
      const html = `
        <div style="font-family:Inter,system-ui,sans-serif;padding:6px 4px;color:#0f172a;line-height:1.4;">
          <div style="font-size:10px;font-weight:800;color:#0284c7;background:#e0f2fe;padding:2px 6px;border-radius:4px;display:inline-block;">
            🟢 LIVE RESCUE TEAM (YOU)
          </div>
          <div style="font-size:13px;font-weight:700;margin-top:4px;color:#0f172a;">Active Device GPS</div>
          <div style="font-size:11px;color:#475569;margin-top:2px;">
            Coordinates: <strong>${liveCoords.lat.toFixed(5)}, ${liveCoords.lng.toFixed(5)}</strong>
          </div>
          <div style="font-size:11px;color:#475569;">
            Accuracy: <strong>±${liveCoords.accuracy || 'N/A'}m</strong>
            ${liveCoords.speed ? ` • Speed: <strong>${liveCoords.speed} km/h</strong>` : ''}
          </div>
          <div style="font-size:10px;color:#64748b;margin-top:4px;">
            Last updated: ${new Date(liveCoords.timestamp).toLocaleTimeString()}
          </div>
        </div>
      `;
      infoWindowRef.current.setContent(html);
      infoWindowRef.current.open(map, teamMarker);
    });

    teamMarkerRef.current = teamMarker;

    return () => {
      if (teamMarkerRef.current) teamMarkerRef.current.setMap(null);
      if (teamAccuracyCircleRef.current) teamAccuracyCircleRef.current.setMap(null);
    };
  }, [liveCoords]);

  const handleRecenterCity = () => {
    if (mapInstanceRef.current && city?.center) {
      mapInstanceRef.current.panTo(city.center);
      mapInstanceRef.current.setZoom(city.zoom || 13);
    }
  };

  const handleRecenterTeam = () => {
    if (mapInstanceRef.current && liveCoords?.lat) {
      mapInstanceRef.current.panTo({ lat: liveCoords.lat, lng: liveCoords.lng });
      mapInstanceRef.current.setZoom(15);
    }
  };

  return (
    <div style={{ position: 'relative', width: '100%', height: '100%' }}>
      {/* Google Map Mount Div */}
      <div ref={mapRef} style={{ width: '100%', height: '100%' }} />

      {/* Floating Tactical Controls */}
      <div
        style={{
          position: 'absolute',
          top: 14,
          right: 14,
          zIndex: 10,
          display: 'flex',
          flexDirection: 'column',
          gap: 6,
        }}
      >
        {/* Map Type Switcher */}
        <div
          style={{
            background: 'rgba(17, 24, 39, 0.92)',
            backdropFilter: 'blur(8px)',
            border: '1px solid var(--clr-border)',
            borderRadius: 'var(--radius-sm)',
            padding: 3,
            display: 'flex',
            boxShadow: 'var(--shadow-md)',
          }}
        >
          <button
            onClick={() => setMapType('roadmap')}
            style={{
              background: mapType === 'roadmap' ? 'var(--clr-primary)' : 'transparent',
              color: mapType === 'roadmap' ? '#fff' : 'var(--clr-text-dim)',
              border: 'none',
              padding: '4px 8px',
              borderRadius: 4,
              fontSize: 11,
              fontWeight: 600,
              cursor: 'pointer',
              transition: 'var(--transition-fast)',
            }}
            title="Dark tactical emergency vector map"
          >
            🗺️ Tactical
          </button>
          <button
            onClick={() => setMapType('hybrid')}
            style={{
              background: mapType === 'hybrid' ? 'var(--clr-primary)' : 'transparent',
              color: mapType === 'hybrid' ? '#fff' : 'var(--clr-text-dim)',
              border: 'none',
              padding: '4px 8px',
              borderRadius: 4,
              fontSize: 11,
              fontWeight: 600,
              cursor: 'pointer',
              transition: 'var(--transition-fast)',
            }}
            title="Satellite imagery with roads"
          >
            🛰️ Satellite
          </button>
        </div>

        {/* Traffic Layer Toggle */}
        <button
          onClick={() => setShowTraffic(!showTraffic)}
          style={{
            background: showTraffic ? 'rgba(34, 197, 94, 0.2)' : 'rgba(17, 24, 39, 0.92)',
            backdropFilter: 'blur(8px)',
            border: `1px solid ${showTraffic ? 'rgba(34, 197, 94, 0.5)' : 'var(--clr-border)'}`,
            color: showTraffic ? '#4ade80' : 'var(--clr-text-dim)',
            borderRadius: 'var(--radius-sm)',
            padding: '5px 9px',
            fontSize: 11,
            fontWeight: 600,
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: 6,
            boxShadow: 'var(--shadow-md)',
          }}
          title="Toggle Google Maps Live Traffic Layer"
        >
          🚦 Traffic {showTraffic ? 'ON' : 'OFF'}
        </button>

        {/* Recenter City Button */}
        <button
          onClick={handleRecenterCity}
          style={{
            background: 'rgba(17, 24, 39, 0.92)',
            backdropFilter: 'blur(8px)',
            border: '1px solid var(--clr-border)',
            color: 'var(--clr-text)',
            borderRadius: 'var(--radius-sm)',
            padding: '5px 9px',
            fontSize: 11,
            fontWeight: 500,
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: 6,
            boxShadow: 'var(--shadow-md)',
          }}
          title={`Center on ${city?.name || 'City'}`}
        >
          🎯 Focus {city?.name || 'City'}
        </button>

        {/* Recenter Team Button (if tracking active) */}
        {liveCoords?.lat && (
          <button
            onClick={handleRecenterTeam}
            style={{
              background: 'rgba(6, 182, 212, 0.2)',
              backdropFilter: 'blur(8px)',
              border: '1px solid rgba(6, 182, 212, 0.5)',
              color: '#38bdf8',
              borderRadius: 'var(--radius-sm)',
              padding: '5px 9px',
              fontSize: 11,
              fontWeight: 600,
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: 6,
              boxShadow: 'var(--shadow-md)',
            }}
            title="Center map on live rescue team"
          >
            🛰️ My Team
          </button>
        )}
      </div>

      {/* Powered by Google Maps watermark badge */}
      <div
        style={{
          position: 'absolute',
          bottom: 18,
          left: 14,
          zIndex: 10,
          background: 'rgba(11, 15, 26, 0.85)',
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
        <span style={{ color: '#38bdf8' }}>Google Maps Platform</span>
        <span>•</span>
        <span>Emergency Road Network</span>
      </div>
    </div>
  );
}
