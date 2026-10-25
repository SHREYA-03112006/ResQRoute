import React, { useState } from 'react';
import StatusBadge from './StatusBadge';

/**
 * ReportCard — displays a single road-condition report with:
 * - Road name and status badge
 * - Match status (MATCHED, WEAK_MATCH, CONFLICT, UNMATCHED)
 * - Matched graph edge indicator
 * - Confidence score bar and breakdown
 * - Source authority badge and link
 * - Interactive dispatcher review actions
 */
export default function ReportCard({ report, onReview }) {
  const [showReview, setShowReview] = useState(false);
  const [reviewNotes, setReviewNotes] = useState(report.reviewNotes || '');
  const [statusOverride, setStatusOverride] = useState(report.reportedStatus || 'UNCERTAIN');
  const [saving, setSaving] = useState(false);

  const confidencePercent = Math.round((report.confidenceScore || 0) * 100);
  const confidenceClass =
    confidencePercent >= 70 ? 'confidence-high'
    : confidencePercent >= 40 ? 'confidence-medium'
    : 'confidence-low';

  const formatTime = (iso) => {
    if (!iso) return null;
    try {
      const d = new Date(iso);
      return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } catch {
      return iso;
    }
  };

  const statusCls = report.reportedStatus?.toLowerCase() || 'unknown';

  const handleSaveReview = async (flagged) => {
    setSaving(true);
    try {
      if (onReview) {
        await onReview(report.id, {
          flaggedForReview: flagged,
          reviewNotes: reviewNotes,
          statusOverride: statusOverride,
        });
      }
      setShowReview(false);
    } catch (err) {
      console.error('Failed to submit review:', err);
    } finally {
      setSaving(false);
    }
  };

  const matchBadgeColor =
    report.matchStatus === 'MATCHED' ? '#22c55e'
    : report.matchStatus === 'CONFLICT' ? '#ef4444'
    : report.matchStatus === 'WEAK_MATCH' ? '#f59e0b'
    : '#94a3b8';

  return (
    <div className={`report-card status-${statusCls} animate-in`} style={{ position: 'relative' }}>
      <div className="report-card-header">
        <div>
          <div className="report-road-name">{report.reportedRoadName}</div>
          {report.normalisedRoadName && report.normalisedRoadName !== report.reportedRoadName && (
            <div style={{ fontSize: 10, color: 'var(--clr-text-muted)', fontFamily: 'JetBrains Mono, monospace' }}>
              OSM Code: {report.normalisedRoadName}
            </div>
          )}
        </div>
        <StatusBadge status={report.reportedStatus} />
      </div>

      {report.locationDescription && (
        <div className="report-location">📍 {report.locationDescription}</div>
      )}

      {/* Match & Edge Status Indicator */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 6, margin: '6px 0', fontSize: 11 }}>
        <span
          style={{
            background: `${matchBadgeColor}20`,
            color: matchBadgeColor,
            border: `1px solid ${matchBadgeColor}40`,
            borderRadius: 4,
            padding: '1px 6px',
            fontSize: 10,
            fontWeight: 700,
            letterSpacing: 0.5,
          }}
        >
          {report.matchStatus || 'UNMATCHED'}
        </span>
        {report.matchedEdgeId && (
          <span style={{ color: 'var(--clr-text-muted)', fontSize: 10 }}>
            Edge #{report.matchedEdgeId}
          </span>
        )}
      </div>

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
          <span className="report-meta-item" style={{ color: 'var(--clr-flooded)', fontWeight: 600 }}>
            ⚑ Review Flagged
          </span>
        )}
      </div>

      {report.reviewNotes && (
        <div style={{ marginTop: 6, fontSize: 11, background: 'rgba(255,255,255,0.03)', padding: '4px 8px', borderRadius: 4, color: 'var(--clr-text-muted)', borderLeft: '2px solid var(--clr-primary)' }}>
          {report.reviewNotes}
        </div>
      )}

      {/* Dispatcher Review Action Toggle */}
      <div style={{ marginTop: 8, display: 'flex', justifyContent: 'flex-end' }}>
        <button
          onClick={() => setShowReview(!showReview)}
          style={{
            background: 'none',
            border: 'none',
            color: 'var(--clr-primary)',
            fontSize: 11,
            cursor: 'pointer',
            padding: '2px 6px',
            borderRadius: 4,
            fontWeight: 600,
          }}
        >
          {showReview ? '▲ Close Review' : '✍️ Dispatcher Review'}
        </button>
      </div>

      {/* Inline Review Drawer */}
      {showReview && (
        <div style={{ marginTop: 8, padding: 10, background: 'var(--clr-surface-2)', border: '1px solid var(--clr-border)', borderRadius: 6 }}>
          <div style={{ fontSize: 11, fontWeight: 700, marginBottom: 6 }}>Review & Route Override</div>
          <div style={{ marginBottom: 6 }}>
            <label style={{ fontSize: 10, color: 'var(--clr-text-muted)', display: 'block', marginBottom: 2 }}>Condition Status</label>
            <select
              value={statusOverride}
              onChange={(e) => setStatusOverride(e.target.value)}
              className="form-select"
              style={{ fontSize: 11, padding: '4px 6px' }}
            >
              <option value="OPEN">OPEN (Clear)</option>
              <option value="FLOODED">FLOODED (Heavy penalty)</option>
              <option value="BLOCKED">BLOCKED (Exclude from routing)</option>
              <option value="UNCERTAIN">UNCERTAIN (Buffer penalty)</option>
            </select>
          </div>
          <div style={{ marginBottom: 6 }}>
            <label style={{ fontSize: 10, color: 'var(--clr-text-muted)', display: 'block', marginBottom: 2 }}>Dispatcher Audit Notes</label>
            <textarea
              value={reviewNotes}
              onChange={(e) => setReviewNotes(e.target.value)}
              placeholder="Enter dispatcher verification notes..."
              rows={2}
              style={{
                width: '100%',
                background: 'var(--clr-surface-1)',
                border: '1px solid var(--clr-border)',
                color: 'var(--clr-text)',
                borderRadius: 4,
                padding: 6,
                fontSize: 11,
                resize: 'none',
              }}
            />
          </div>
          <div style={{ display: 'flex', gap: 6, justifyContent: 'flex-end' }}>
            <button
              onClick={() => handleSaveReview(false)}
              disabled={saving}
              className="btn btn-primary"
              style={{ fontSize: 10, padding: '4px 10px' }}
            >
              {saving ? 'Saving...' : '✓ Approve & Update'}
            </button>
            <button
              onClick={() => handleSaveReview(true)}
              disabled={saving}
              className="btn"
              style={{ fontSize: 10, padding: '4px 10px', background: 'rgba(239,68,68,0.15)', color: '#f87171', border: '1px solid rgba(239,68,68,0.3)' }}
            >
              ⚑ Keep Flagged
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
