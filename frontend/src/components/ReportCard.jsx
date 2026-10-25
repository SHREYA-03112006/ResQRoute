import React, { useState } from 'react';
import StatusBadge from './StatusBadge';

const CATEGORY_META = {
  FIRE: { label: 'Fire Hazard', icon: '🔥', color: '#ef4444', bg: 'rgba(239, 68, 68, 0.15)' },
  FLOOD: { label: 'Flood / Inundation', icon: '🌊', color: '#0ea5e9', bg: 'rgba(14, 165, 233, 0.15)' },
  LANDSLIDE: { label: 'Landslide / Rockfall', icon: '⛰️', color: '#f59e0b', bg: 'rgba(245, 158, 11, 0.15)' },
  ROAD_BLOCKAGE: { label: 'Road Blockage', icon: '🚫', color: '#e11d48', bg: 'rgba(225, 29, 72, 0.15)' },
  BUILDING_COLLAPSE: { label: 'Building Collapse', icon: '🏚️', color: '#a855f7', bg: 'rgba(168, 85, 247, 0.15)' },
  OTHER: { label: 'Emergency Alert', icon: '⚠️', color: '#64748b', bg: 'rgba(100, 116, 139, 0.15)' },
};

const VERIFICATION_META = {
  NEW: { label: 'NEW INCIDENT', color: '#22c55e', bg: 'rgba(34, 197, 94, 0.15)' },
  UPDATED: { label: 'UPDATED', color: '#38bdf8', bg: 'rgba(56, 189, 248, 0.15)' },
  DUPLICATE: { label: 'DUPLICATE', color: '#94a3b8', bg: 'rgba(148, 163, 184, 0.15)' },
  UNVERIFIED: { label: 'UNVERIFIED', color: '#fbbf24', bg: 'rgba(251, 191, 36, 0.15)' },
};

/**
 * ReportCard — displays a single road-condition report with Phase 2 Disaster Intelligence:
 * - Incident Title & Disaster Category badge
 * - Verification status (NEW, UPDATED, DUPLICATE, UNVERIFIED)
 * - Road name, location description & coordinates/area status
 * - Match status (MATCHED, WEAK_MATCH, CONFLICT, UNMATCHED)
 * - Matched graph edge indicator
 * - Confidence score bar and explanation
 * - Freshness: Observed time & Retrieved time
 * - Source authority badge and verified link
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
  const categoryKey = report.disasterCategory || (report.type ? report.type.replace('FLASH_', '').replace('WATERLOGGING', 'FLOOD') : 'OTHER');
  const catMeta = CATEGORY_META[categoryKey] || CATEGORY_META.OTHER;
  const verMeta = VERIFICATION_META[report.verificationStatus] || (report.isDemo ? VERIFICATION_META.NEW : VERIFICATION_META.NEW);

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

  const isLive = report.dataMode === 'LIVE';

  return (
    <div className={`report-card status-${statusCls} animate-in`} style={{ position: 'relative' }}>
      {/* Category & Mode Header Banner */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 6, gap: 4 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6, flexWrap: 'wrap' }}>
          <span
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: 4,
              background: catMeta.bg,
              color: catMeta.color,
              border: `1px solid ${catMeta.color}40`,
              borderRadius: 4,
              padding: '2px 7px',
              fontSize: 10,
              fontWeight: 700,
              letterSpacing: 0.4,
              textTransform: 'uppercase',
            }}
          >
            {catMeta.icon} {catMeta.label}
          </span>

          {report.verificationStatus && (
            <span
              style={{
                background: verMeta.bg,
                color: verMeta.color,
                border: `1px solid ${verMeta.color}40`,
                borderRadius: 4,
                padding: '2px 6px',
                fontSize: 9,
                fontWeight: 700,
              }}
              title={`Verification pipeline classification: ${verMeta.label}`}
            >
              {verMeta.label}
            </span>
          )}
        </div>

        {/* Live vs Demo Badge */}
        <span
          style={{
            background: isLive ? 'rgba(34, 197, 94, 0.15)' : 'rgba(234, 179, 8, 0.15)',
            color: isLive ? '#22c55e' : '#eab308',
            border: `1px solid ${isLive ? 'rgba(34, 197, 94, 0.3)' : 'rgba(234, 179, 8, 0.3)'}`,
            borderRadius: 4,
            padding: '2px 6px',
            fontSize: 9,
            fontWeight: 800,
            letterSpacing: 0.5,
          }}
          title={isLive ? 'Live TinyFish intelligence extraction' : 'Sample/Demo intelligence dataset for evaluation'}
        >
          {isLive ? '🟢 LIVE TINYFISH' : '🟡 DEMO / SAMPLE'}
        </span>
      </div>

      {/* Incident Title & Road Status */}
      <div className="report-card-header">
        <div>
          <div className="report-road-name" style={{ fontSize: 13, fontWeight: 700, color: 'var(--clr-text-bright)' }}>
            {report.incidentTitle || report.reportedRoadName}
          </div>
          {report.reportedRoadName && report.incidentTitle && report.incidentTitle !== report.reportedRoadName && (
            <div style={{ fontSize: 11, color: 'var(--clr-text-muted)', marginTop: 2 }}>
              Corridor: <span style={{ color: 'var(--clr-text)' }}>{report.reportedRoadName}</span>
            </div>
          )}
        </div>
        <StatusBadge status={report.reportedStatus} />
      </div>

      {report.locationDescription && (
        <div className="report-location" style={{ fontSize: 11, color: 'var(--clr-text-muted)', marginTop: 4 }}>
          📍 {report.locationDescription}
        </div>
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
          title={`Graph matching status: ${report.matchStatus || 'UNMATCHED'}`}
        >
          {report.matchStatus || 'UNMATCHED'}
        </span>
        {report.matchedEdgeId && (
          <span style={{ color: 'var(--clr-text-muted)', fontSize: 10 }}>
            Edge #{report.matchedEdgeId}
          </span>
        )}
        {report.severity && (
          <span
            style={{
              marginLeft: 'auto',
              fontSize: 10,
              fontWeight: 700,
              color: report.severity === 'CRITICAL' ? '#ef4444' : report.severity === 'HIGH' ? '#f59e0b' : '#38bdf8',
            }}
          >
            {report.severity} SEVERITY
          </span>
        )}
      </div>

      {/* Confidence bar & explanation */}
      <div className="confidence-bar" title={`Algorithmic confidence based on source authority, location evidence, and freshness: ${confidencePercent}%`}>
        <div className="confidence-bar-track">
          <div
            className={`confidence-bar-fill ${confidenceClass}`}
            style={{ width: `${confidencePercent}%` }}
          />
        </div>
        <span className={`confidence-label ${confidenceClass}`}>{confidencePercent}%</span>
      </div>

      <div className="report-meta">
        {report.observedAt && (
          <span className="report-meta-item" title="Incident observation or alert published time">
            <span>🕐</span>
            <span>{formatTime(report.observedAt)}</span>
          </span>
        )}
        {report.retrievedAt && (
          <span className="report-meta-item" title="Pipeline retrieval & extraction timestamp">
            <span>⚡</span>
            <span>Sync: {formatTime(report.retrievedAt)}</span>
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
