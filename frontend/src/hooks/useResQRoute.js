import { useState, useEffect, useCallback } from 'react';
import { getSystemStatus, getReports, getLocations } from '../services/api';

/**
 * Hook that polls the backend system-status endpoint every 30 seconds.
 * @returns {{ status: Object|null, loading: boolean, error: string|null, refetch: Function }}
 */
export function useSystemStatus() {
  const [status, setStatus]   = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError]     = useState(null);

  const fetchStatus = useCallback(async () => {
    try {
      const data = await getSystemStatus();
      setStatus(data);
      setError(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    let ignore = false;
    getSystemStatus()
      .then((data) => {
        if (!ignore) {
          setStatus(data);
          setError(null);
          setLoading(false);
        }
      })
      .catch((err) => {
        if (!ignore) {
          setError(err.message);
          setLoading(false);
        }
      });

    const id = setInterval(fetchStatus, 30000);
    return () => {
      ignore = true;
      clearInterval(id);
    };
  }, [fetchStatus]);

  return { status, loading, error, refetch: fetchStatus };
}

/**
 * Hook that fetches road-condition reports and re-polls every 60 seconds.
 * @param {Object} params - Query params forwarded to the reports endpoint
 * @returns {{ reports: Array, loading: boolean, error: string|null, refetch: Function }}
 */
export function useReports(params = {}) {
  const [reports, setReports] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError]     = useState(null);

  const fetchReports = useCallback(async () => {
    try {
      const data = await getReports(params);
      setReports(data);
      setError(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [params]);

  useEffect(() => {
    let ignore = false;
    getReports(params)
      .then((data) => {
        if (!ignore) {
          setReports(data);
          setError(null);
          setLoading(false);
        }
      })
      .catch((err) => {
        if (!ignore) {
          setError(err.message);
          setLoading(false);
        }
      });

    const id = setInterval(fetchReports, 60000);
    return () => {
      ignore = true;
      clearInterval(id);
    };
  }, [fetchReports, params]);

  return { reports, loading, error, refetch: fetchReports };
}

/**
 * Hook that fetches all locations once on mount.
 * @returns {{ locations: Array, loading: boolean, error: string|null }}
 */
export function useLocations() {
  const [locations, setLocations] = useState([]);
  const [loading, setLoading]     = useState(true);
  const [error, setError]         = useState(null);

  useEffect(() => {
    let ignore = false;
    getLocations()
      .then((data) => {
        if (!ignore) {
          setLocations(data);
          setError(null);
          setLoading(false);
        }
      })
      .catch((err) => {
        if (!ignore) {
          setError(err.message);
          setLoading(false);
        }
      });

    return () => {
      ignore = true;
    };
  }, []);

  return { locations, loading, error };
}
