import { useState, useEffect } from 'react';
import { Loader } from '@googlemaps/js-api-loader';

/**
 * useGoogleMaps — dynamically loads Google Maps JavaScript API.
 *
 * Reads VITE_GOOGLE_MAPS_API_KEY safely from the environment.
 * If the key is missing or invalid, sets appropriate status flags so a graceful
 * fallback UI can be displayed without crashing.
 */
export function useGoogleMaps() {
  const [isLoaded, setIsLoaded] = useState(false);
  const [loadError, setLoadError] = useState(null);
  const [googleInstance, setGoogleInstance] = useState(null);

  const rawKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY;
  // Key is valid if it exists, is not empty, and not an unconfigured placeholder
  const apiKey = (typeof rawKey === 'string' && rawKey.trim() && !rawKey.includes('your_google_maps_api_key'))
    ? rawKey.trim()
    : null;

  const hasKey = Boolean(apiKey);

  useEffect(() => {
    // If already loaded in window
    if (typeof window !== 'undefined' && window.google && window.google.maps) {
      setGoogleInstance(window.google);
      setIsLoaded(true);
      return;
    }

    if (!hasKey) {
      setIsLoaded(false);
      setLoadError(null);
      return;
    }

    // Intercept Google Maps authentication failure callback
    const originalAuthFailure = window.gm_authFailure;
    window.gm_authFailure = () => {
      console.warn('[ResQRoute] Google Maps authentication failed. Please verify your VITE_GOOGLE_MAPS_API_KEY.');
      setLoadError('Google Maps API key is invalid or unauthorized for this domain.');
      setIsLoaded(false);
      if (typeof originalAuthFailure === 'function') {
        originalAuthFailure();
      }
    };

    const loader = new Loader({
      apiKey,
      version: 'weekly',
      libraries: ['places', 'geometry'],
    });

    let isMounted = true;

    loader
      .load()
      .then((google) => {
        if (isMounted) {
          setGoogleInstance(google);
          setIsLoaded(true);
          setLoadError(null);
        }
      })
      .catch((err) => {
        if (isMounted) {
          console.error('[ResQRoute] Failed to load Google Maps:', err);
          setLoadError(err.message || 'Failed to load Google Maps JavaScript API.');
          setIsLoaded(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, [hasKey, apiKey]);

  return {
    isLoaded,
    loadError,
    hasKey,
    google: googleInstance || (typeof window !== 'undefined' ? window.google : null),
  };
}
