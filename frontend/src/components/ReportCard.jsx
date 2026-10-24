import React from 'react';
import StatusBadge from './StatusBadge';

/**
 * ReportCard — displays a single road-condition report with:
 * - Road name and status badge
 * - Location description
 * - Source name and URL
 * - Confidence score bar
 * - Data-mode label (SAMPLE / LIVE)
 * - Flag for review toggle
 */
export default function ReportCard({ report, onReview }) {
  const confidencePercent = Math.round((report.confidenceScore || 0) * 100);
  const confidenceClass =
    confidencePercent >= 70 ? 'confidence-high'
    : confidencePercent >= 40 ? 'confidence-medium'
    : 'confidence-low';

  const formatTime = (iso) => {
    if (!iso) return null;
    try {
      const d = new Date(iso);
      const diff = Math.floor((Date.now() - d) / 60000);
      if (diff < 60) return `${diff}m ago`;
      if (diff < 1440) return `${Math.floor(diff / 60)}h ago`;
      return d.toLocaleDateString();
    } catch {
      return iso;
    }
  };

  const statusCls = report.reportedStatus?.toLowerCase() || 'unknown';

  return (
    <div className={`report-card status-${statusCls} animate-in`}>
      <div className="report-card-header">
        <div className="report-road-name">{report.reportedRoadName}</div>
        <StatusBadge status={report.reportedStatus} />
      </div>

      {report.locationDescription && (
        <div className="report-location">📍 {report.locationDescription}</div>
      )}

      {/* Confidence bar */}
      <div className="confidence-bar">
        <div className="confidence-bar-track">
          <div
            className={`confidence-bar-fill ${confidenceClass}`}
            style={{ width: `${confidencePercent}%` }}
          />
        </div>
        <span className={`confidence-label ${confidenceClass}`}>{confidencePercent}%</span>
      </div>

      <div className="report-meta">
        {report.dataMode && (
          <span className="report-meta-item">
            <span>{report.dataMode === 'LIVE' ? '🟢' : '🟡'}</span>
            <span style={{ fontWeight: 700, fontSize: '10px' }}>{report.dataMode}</span>
          </span>
        )}
        {report.observedAt && (
          <span className="report-meta-item">
            <span>🕐</span>
            <span>{formatTime(report.observedAt)}</span>
          </span>
        )}
        {report.sourceName && (
          <span className="report-meta-item" title={report.sourceName}>
            <span>📰</span>
            {report.sourceUrl ? (
              <a
                href={report.sourceUrl}
                target="_blank"
                rel="noopener noreferrer"
                style={{ color: 'var(--clr-primary)', textDecoration: 'none', maxWidth: 140, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', display: 'inline-block' }}
              >
                {report.sourceName.replace('[SAMPLE] ', '')}
              </a>
            ) : (
              <span className="truncate" style={{ maxWidth: 140 }}>{report.sourceName.replace('[SAMPLE] ', '')}</span>
            )}
          </span>
        )}
        {report.flaggedForReview && (
          <span className="report-meta-item" style={{ color: 'var(--clr-flooded)' }}>
            ⚑ Flagged for review
          </span>
        )}
        {report.matchStatus === 'UNMATCHED' && (
          <span className="report-meta-item" style={{ color: 'var(--clr-text-muted)', fontStyle: 'italic' }}>
            Road match pending
          </span>
        )}
      </div>
    </div>
  );
}
