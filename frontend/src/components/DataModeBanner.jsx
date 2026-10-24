import React from 'react';

/**
 * DataModeBanner — prominently labels the current data source
 * so dispatchers are never misled about whether data is live or sample.
 *
 * @param {string} mode - 'LIVE' | 'SAMPLE' | 'MOCK'
 * @param {string} lastRefresh - ISO timestamp string
 */
export default function DataModeBanner({ mode, lastRefresh }) {
  const isLive = mode === 'LIVE';

  const formatTime = (iso) => {
    if (!iso) return 'unknown';
    try {
      return new Date(iso).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } catch {
      return iso;
    }
  };

  return (
    <div className={`data-mode-banner ${isLive ? 'data-mode-live' : 'data-mode-sample'}`}>
      <span>{isLive ? '🟢' : '🟡'}</span>
      <span>
        {isLive
          ? `LIVE DATA — last refresh ${formatTime(lastRefresh)}`
          : `⚠ SAMPLE DATA — not verified, not live. For demonstration only.`}
      </span>
    </div>
  );
}
