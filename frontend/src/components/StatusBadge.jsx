import React from 'react';

const STATUS_CONFIG = {
  OPEN:      { label: 'Open',      cls: 'status-open',      dotCls: 'dot-open'     },
  FLOODED:   { label: 'Flooded',   cls: 'status-flooded',   dotCls: 'dot-flooded'  },
  BLOCKED:   { label: 'Blocked',   cls: 'status-blocked',   dotCls: 'dot-blocked'  },
  UNCERTAIN: { label: 'Uncertain', cls: 'status-uncertain', dotCls: 'dot-uncertain' },
  UNKNOWN:   { label: 'Unknown',   cls: 'status-unknown',   dotCls: 'dot-unknown'  },
};

/**
 * StatusBadge — renders a coloured pill for a road status value.
 * @param {string} status - One of OPEN|FLOODED|BLOCKED|UNCERTAIN|UNKNOWN
 */
export default function StatusBadge({ status }) {
  const cfg = STATUS_CONFIG[status?.toUpperCase()] || STATUS_CONFIG.UNKNOWN;
  return (
    <span className={`status-badge ${cfg.cls}`}>
      <span className={`status-badge-dot ${cfg.dotCls}`} />
      {cfg.label}
    </span>
  );
}
