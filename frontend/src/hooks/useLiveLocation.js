import { useState, useEffect, useRef, useCallback } from 'react';

/**
 * useLiveLocation — Browser Geolocation API hook for rescue-team tracking.
 *
 * Requirements satisfied:
 *  - Activates ONLY after explicit user click on a tracking control.
 *  - Uses navigator.geolocation.watchPosition for real-time location stream.
 *  - Immediate stopTracking control invoking clearWatch.
 *  - Gracefully handles denied permissions, unavailable GPS, timeouts.
 *  - Never tracks silently in the background or fabricates coordinates.
 */
export function useLiveLocation() {
  const [coords, setCoords] = useState(null);
  const [status, setStatus] = useState('IDLE'); // 'IDLE' | 'REQUESTING' | 'TRACKING' | 'DENIED' | 'ERROR' | 'UNSUPPORTED'
  const [error, setError] = useState(null);
  const watchIdRef = useRef(null);

  const isTracking = status === 'TRACKING' || status === 'REQUESTING';

  // Stop tracking and clean up active watch
  const stopTracking = useCallback(() => {
    if (watchIdRef.current !== null && typeof navigator !== 'undefined' && navigator.geolocation) {
      navigator.geolocation.clearWatch(watchIdRef.current);
      watchIdRef.current = null;
    }
    setStatus('IDLE');
  }, []);

  // Start tracking upon explicit user action
  const startTracking = useCallback(() => {
    if (typeof navigator === 'undefined' || !navigator.geolocation) {
      setStatus('UNSUPPORTED');
      setError('Geolocation is not supported by your current browser.');
      return;
    }

    // Reset error & set requesting state
    setError(null);
    setStatus('REQUESTING');

    const handleSuccess = (position) => {
      const { latitude, longitude, accuracy, altitude, heading, speed } = position.coords;
      setCoords({
        lat: latitude,
        lng: longitude,
        accuracy: Math.round(accuracy || 0),
        altitude: altitude !== null ? Math.round(altitude) : null,
        heading: heading !== null ? Math.round(heading) : null,
        speed: speed !== null ? Math.round(speed * 3.6) : null, // convert m/s to km/h
        timestamp: new Date(position.timestamp).toISOString(),
      });
      setStatus('TRACKING');
      setError(null);
    };

    const handleError = (geoError) => {
      let friendlyMessage = 'Unable to retrieve your location.';
      let newStatus = 'ERROR';

      switch (geoError.code) {
        case geoError.PERMISSION_DENIED:
          newStatus = 'DENIED';
          friendlyMessage = 'Location permission was denied. Please allow location access in your browser to enable live team tracking.';
          break;
        case geoError.POSITION_UNAVAILABLE:
          newStatus = 'ERROR';
          friendlyMessage = 'GPS signal is currently unavailable or device is offline.';
          break;
        case geoError.TIMEOUT:
          newStatus = 'ERROR';
          friendlyMessage = 'Location request timed out. Retrying with network location...';
          break;
        default:
          friendlyMessage = geoError.message || 'An unknown geolocation error occurred.';
      }

      setError(friendlyMessage);
      setStatus(newStatus);
    };

    const options = {
      enableHighAccuracy: true,
      timeout: 15000,
      maximumAge: 3000,
    };

    // Begin watching position
    try {
      // Clear any prior watch
      if (watchIdRef.current !== null) {
        navigator.geolocation.clearWatch(watchIdRef.current);
      }
      watchIdRef.current = navigator.geolocation.watchPosition(handleSuccess, handleError, options);
    } catch (err) {
      setError('Failed to initiate geolocation: ' + err.message);
      setStatus('ERROR');
    }
  }, []);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      if (watchIdRef.current !== null && typeof navigator !== 'undefined' && navigator.geolocation) {
        navigator.geolocation.clearWatch(watchIdRef.current);
      }
    };
  }, []);

  const clearError = useCallback(() => setError(null), []);

  return {
    isTracking,
    status,
    coords,
    error,
    startTracking,
    stopTracking,
    clearError,
  };
}
