/**
 * Violation type constants and helpers — single source of truth.
 */

export const VIOLATION_LABELS = {
  wrong_direction: '逆行',
  illegal_parking: '违停',
  speeding: '超速',
  congestion: '拥堵'
}

export const VIOLATION_TITLES = {
  wrong_direction: '逆行检测',
  illegal_parking: '违停检测',
  speeding: '超速检测',
  congestion: '拥堵预警'
}

export const VIOLATION_COLORS = {
  wrong_direction: '#ef4444',
  illegal_parking: '#f59e0b',
  speeding: '#dc2626',
  congestion: '#3b82f6'
}

export const VIOLATION_TAG_TYPES = {
  wrong_direction: 'danger',
  illegal_parking: 'warning',
  speeding: 'danger',
  congestion: 'info'
}

export const VIOLATION_ICONS = {
  wrong_direction: 'Sort',
  illegal_parking: 'Timer',
  speeding: 'Odometer',
  congestion: 'Warning'
}

/**
 * Severity: critical > high > medium > low
 */
export function getViolationSeverity(type, speedKmh, speedLimit = 60) {
  if (type === 'speeding' && speedKmh && speedKmh > speedLimit * 1.5) return 'critical'
  if (type === 'speeding' && speedKmh && speedKmh > speedLimit * 1.3) return 'critical'
  if (type === 'wrong_direction') return 'high'
  if (type === 'speeding') return 'high'
  if (type === 'illegal_parking') return 'medium'
  return 'low'
}

export function getSeverityColor(severity) {
  return {
    critical: '#7f1d1d',
    high: '#dc2626',
    medium: '#f59e0b',
    low: '#3b82f6'
  }[severity] || '#64748b'
}

export const VIOLATION_TYPES = ['wrong_direction', 'illegal_parking', 'speeding', 'congestion']
